package com.unimarket.ai.service.impl;

import com.unimarket.ai.dto.GoodsPriceEstimateDTO;
import com.unimarket.ai.service.AiPricingService;
import com.unimarket.ai.vo.GoodsPriceEstimateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 估价能力实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPricingServiceImpl implements AiPricingService {

    private final ChatClient chatClient;

    @Override
    public GoodsPriceEstimateVO estimatePrice(GoodsPriceEstimateDTO dto) {
        log.info("开始AI估价: title={}, categoryId={}", dto.getTitle(), dto.getCategoryId());

        GoodsPriceEstimateVO result = new GoodsPriceEstimateVO();
        try {
            String prompt = String.format("""
                你是一个专业的二手商品估价专家。请根据以下待估价商品信息和同类商品的市场数据，给出合理的建议售价。

                【待估价商品信息】
                - 标题：%s
                - 描述：%s
                - 成色：%d成新

                【市场参考数据】
                %s
                请综合分析待估价商品与参考商品的相似度、成色差异、市场行情等因素，给出建议价格。

                请严格按照以下JSON格式返回结果（不要添加任何其他文字）：
                {"price": 建议价格数字, "reason": "估价理由，说明参考了哪些因素"}
                """,
                    dto.getTitle() != null ? dto.getTitle() : "",
                    dto.getDescription() != null ? dto.getDescription() : "",
                    dto.getItemCondition() != null ? dto.getItemCondition() : 9,
                    dto.getReferenceData() != null && !dto.getReferenceData().isEmpty()
                        ? dto.getReferenceData()
                        : "暂无同类商品参考数据");

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个专业的二手商品估价助手，只返回JSON格式的结果。"));
            messages.add(new UserMessage(prompt));

            String response = chatClient.call(new Prompt(messages)).getResult().getOutput().getContent();
            log.info("AI估价响应: {}", response);

            double price = extractJsonNumber(response, "price", 50.0);
            String reason = extractJsonString(response, "reason");
            if (reason == null || reason.isEmpty()) {
                reason = "基于AI分析给出的建议定价";
            }

            result.setSuggestedPrice(price);
            result.setReferenceCount(0);
            result.setReason(reason);
            return result;
        } catch (Exception e) {
            log.error("AI估价异常", e);
            result.setSuggestedPrice(50.0);
            result.setReferenceCount(0);
            result.setReason("暂无足够的参考数据，建议参考市场行情自行定价");
            return result;
        }
    }

    private String extractJsonString(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\": \"");
            if (start < 0) {
                start = json.indexOf("\"" + key + "\":\"");
            }
            if (start >= 0) {
                start = json.indexOf("\"", start + key.length() + 2) + 1;
                int end = json.indexOf("\"", start);
                while (end > 0 && json.charAt(end - 1) == '\\') {
                    end = json.indexOf("\"", end + 1);
                }
                if (end > start) {
                    return json.substring(start, end).replace("\\\"", "\"").replace("\\n", "\n");
                }
            }
        } catch (Exception e) {
            log.debug("提取JSON字符串失败: key={}", key);
        }
        return "";
    }

    private Double extractJsonNumber(String json, String key, Double defaultValue) {
        try {
            String pattern1 = "\"" + key + "\": ";
            String pattern2 = "\"" + key + "\":";
            int start = json.indexOf(pattern1);
            if (start < 0) {
                start = json.indexOf(pattern2);
                if (start >= 0) {
                    start += pattern2.length();
                }
            } else {
                start += pattern1.length();
            }
            if (start > 0) {
                StringBuilder num = new StringBuilder();
                for (int i = start; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (Character.isDigit(c) || c == '.' || c == '-') {
                        num.append(c);
                    } else if (!num.isEmpty()) {
                        break;
                    }
                }
                if (!num.isEmpty()) {
                    return Double.parseDouble(num.toString());
                }
            }
        } catch (Exception e) {
            log.debug("提取JSON数字失败: key={}", key);
        }
        return defaultValue;
    }
}
