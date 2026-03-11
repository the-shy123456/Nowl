package com.unimarket.ai.service.impl;

import com.unimarket.ai.dto.AiAuditResult;
import com.unimarket.ai.service.AiAuditService;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeTypeUtils;
import java.net.MalformedURLException;
import java.util.List;

/**
 * AI审核Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAuditServiceImpl implements AiAuditService {

    private final ChatClient chatClient;

    @Override
    public AiAuditResult auditText(String content) {
        String promptText = String.format("""
            你是一个大学校园二手交易平台的审核员，审核商品与跑腿任务内容。请判断以下内容的合规性和风险等级。

            审核标准：
            1. 违规内容：色情、暴力、诈骗、违禁品（如管制刀具、药品、烟酒等，化妆品等符合大学生群体的物品给予放行）
            2. 低风险(low)：内容完全合规，无任何风险
            3. 中风险(medium)：内容可能存在争议或需要进一步核实（如价格异常、描述疑似存在骂人、色情信息）
            4. 高风险(high)：内容明确违规，应直接拒绝

            请严格遵循以下JSON格式返回结果（不要包含Markdown代码块）：
            {"safe": true/false, "riskLevel": "low"或"medium"或"high", "reason": "原因说明"}

            内容如下：
            %s
            """, content);
        try {
            String response = chatClient.call(promptText);
            log.info("AI文本审核结果: {}", response);

            // 清理可能存在的Markdown代码块标记
            String jsonStr = response.replace("```json", "").replace("```", "").trim();

            try {
                JSONObject json = JSONUtil.parseObj(jsonStr);
                boolean safe = json.getBool("safe", true);
                String reason = json.getStr("reason", "");
                String riskLevel = json.getStr("riskLevel", safe ? "low" : "high");
                return new AiAuditResult(safe, reason, riskLevel);
            } catch (Exception parseEx) {
                // 如果JSON解析失败，尝试回退到简单的文本匹配
                log.warn("AI审核结果JSON解析失败，尝试文本匹配: {}", jsonStr);
                String upperResponse = jsonStr.toUpperCase();
                if (upperResponse.contains("FALSE") || upperResponse.contains("不合规") || upperResponse.contains("违规")) {
                    return new AiAuditResult(false, "内容疑似违规（解析异常）", "high");
                }
                if (upperResponse.contains("MEDIUM") || upperResponse.contains("疑似") || upperResponse.contains("可能")) {
                    return new AiAuditResult(true, "内容需要人工复核", "medium");
                }
                return new AiAuditResult(true, "", "low");
            }
        } catch (Exception e) {
            log.error("AI文本审核异常，已提交人工审核", e);
            return new AiAuditResult(true, resolveAuditFallbackReason(e, "AI审核服务暂时不可用，已提交人工审核"), "medium");
        }
    }


    @Override
    public AiAuditResult auditImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return new AiAuditResult(true, "", "low");
        }
        log.info("开始AI图片审核: {}", imageUrl);
        try {
            UrlResource imageResource = new UrlResource(imageUrl);
            String promptText = """
                    你是校园二手交易平台的图片审核员。请判断图片是否合规，并输出风险等级。

                    请严格返回以下JSON格式（不要返回Markdown代码块）：
                    {"safe": true/false, "riskLevel": "low"或"medium"或"high", "reason": "原因说明"}

                    风险等级定义：
                    1. low：图片内容清晰且合规（常见校园二手物品均可通过，如耳机、教材、文具、电子产品、生活用品、化妆品等）；
                    2. medium：图片模糊、遮挡、信息不足，或存在疑似风险但证据不充分，需要人工复核；
                    3. high：图片中明确出现色情、暴力血腥或违禁品（枪支、刀具、毒品、易燃易爆品等）。

                    关键要求：
                    - 不能确认违规时，必须返回medium，不能直接判high；
                    - reason需要简洁说明判断依据。
                    """;
            UserMessage userMessage = new UserMessage(
                    promptText,
                    List.of(new Media(MimeTypeUtils.IMAGE_JPEG, imageResource))
            );
            Prompt prompt = new Prompt(userMessage);
            String response = chatClient.call(prompt).getResult().getOutput().getContent();
            log.info("AI图片审核原始响应: {}", response);

            String jsonStr = response.replace("```json", "").replace("```", "").trim();
            try {
                JSONObject json = JSONUtil.parseObj(jsonStr);
                boolean safe = json.getBool("safe", true);
                String riskLevel = normalizeRiskLevel(json.getStr("riskLevel", safe ? "low" : "high"), safe ? "low" : "high");
                String reason = json.getStr("reason", "");

                if ("high".equals(riskLevel)) {
                    if (reason == null || reason.isBlank()) {
                        reason = "图片疑似包含违规内容";
                    }
                    return new AiAuditResult(false, reason, "high");
                }

                if ("medium".equals(riskLevel)) {
                    if (reason == null || reason.isBlank()) {
                        reason = "图片识别结果不确定，建议人工复核";
                    }
                    return new AiAuditResult(true, reason, "medium");
                }

                return new AiAuditResult(true, reason, "low");
            } catch (Exception parseEx) {
                log.warn("AI图片审核结果JSON解析失败，尝试兼容旧格式: {}", jsonStr);
                String cleanResponse = jsonStr.trim().toUpperCase().replaceAll("[^A-Z]", "");
                if ("YES".equals(cleanResponse)) {
                    return new AiAuditResult(true, "", "low");
                }
                if ("NO".equals(cleanResponse)) {
                    return new AiAuditResult(true, "图片疑似存在风险，建议人工复核", "medium");
                }
                return new AiAuditResult(true, "图片识别结果不确定，建议人工复核", "medium");
            }
        } catch (MalformedURLException e) {
            log.error("图片URL格式错误: {}", imageUrl, e);
            return new AiAuditResult(true, "图片地址异常，建议人工复核", "medium");
        } catch (Exception e) {
            log.error("AI图片审核调用失败", e);
            return new AiAuditResult(true, resolveAuditFallbackReason(e, "图片审核服务异常，建议人工复核"), "medium");
        }
    }

    private String resolveAuditFallbackReason(Exception e, String defaultReason) {
        if (e == null) {
            return defaultReason;
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return defaultReason;
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("429") || normalized.contains("overloaded") || normalized.contains("rate limit")) {
            return "AI审核服务繁忙，已转人工复核";
        }
        return defaultReason;
    }

    private String normalizeRiskLevel(String riskLevel, String defaultLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return defaultLevel;
        }
        String normalized = riskLevel.trim().toLowerCase();
        if ("low".equals(normalized) || "medium".equals(normalized) || "high".equals(normalized)) {
            return normalized;
        }
        return defaultLevel;
    }

    @Override
    public Double getPriceSuggestion(String title, String description, String categoryName) {
        String promptText = String.format("你是一个二手交易专家。请根据以下商品信息给出合理的建议售价（单位：元）。只需回答数字。商品标题：%s，描述：%s，分类：%s", title, description, categoryName);
        try {
            String response = chatClient.call(promptText);
            log.info("AI定价建议结果: {}", response);
            return Double.parseDouble(response.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            log.error("AI定价建议异常", e);
            return null;
        }
    }
}

