package com.unimarket.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unimarket.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * AI 对话能力实现（仅负责聊天与 Function Calling）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final RestClient.Builder restClientBuilder;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model}")
    private String model;

    private static final String SYSTEM_PROMPT_TEXT = """
            你叫"Nowl AI"，是UniMarket校园二手交易平台的智能助手。
            你的职责是：
            1. 帮助同学解答关于二手交易、跑腿任务、纠纷处理的问题。
            2. 提供商品检索、推荐相关的问答支持。
            3. 不提供商品估价或审核结论，这类能力仅在发布商品页面提供。
            4. 语气要活泼、亲切，像学长学姐一样。
            5. 始终保持简短的回答，避免长篇大论。
            """;

    @Override
    public String chat(String message, String imageUrl, String historyContext) {
        try {
            // Spring AI 0.8.1 的 OpenAiChatClient 在请求转换时不会把 Media 带到最终请求里，
            // 图片会在框架层被静默丢弃，因此这里对带图对话单独走兼容 OpenAI 的多模态请求。
            if (imageUrl != null && !imageUrl.isEmpty()) {
                log.info("Nowl AI收到图片消息: {}", imageUrl);
                String response = callMultimodalChat(message, imageUrl, historyContext);
                log.info("Nowl AI回复: {}", response);
                return response;
            }

            List<Message> historyMessages = parseHistoryContext(historyContext);

            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(SYSTEM_PROMPT_TEXT));
            promptMessages.addAll(historyMessages);
            promptMessages.add(new UserMessage(message));

            log.info("Nowl AI正在思考... (历史记录数: {})", historyMessages.size());
            String response = chatClient.call(new Prompt(promptMessages)).getResult().getOutput().getContent();
            log.info("Nowl AI回复: {}", response);
            return response;

        } catch (Exception e) {
            log.error("AI服务调用异常", e);
            return "抱歉，Nowl AI现在有点忙，请稍后再试~";
        }
    }

    @Override
    public String chatWithFunctions(
            String message,
            String historyContext,
            String systemPrompt,
            List<FunctionCallback> functionCallbacks
    ) {
        try {
            List<Message> historyMessages = parseHistoryContext(historyContext);

            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(
                    systemPrompt == null || systemPrompt.isBlank()
                            ? SYSTEM_PROMPT_TEXT
                            : systemPrompt
            ));
            promptMessages.addAll(historyMessages);
            promptMessages.add(new UserMessage(Objects.toString(message, "")));

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .withFunctionCallbacks(functionCallbacks == null ? List.of() : functionCallbacks)
                    .build();

            String response = chatClient.call(new Prompt(promptMessages, options))
                    .getResult()
                    .getOutput()
                    .getContent();
            log.info("Nowl AI(Function Calling)回复: {}", response);
            return response;
        } catch (Exception e) {
            log.error("AI Function Calling调用异常", e);
            return "";
        }
    }

    private String callMultimodalChat(String message, String imageUrl, String historyContext) {
        RestClient restClient = restClientBuilder
                .baseUrl(resolveOpenAiApiBaseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", SYSTEM_PROMPT_TEXT
        ));
        messages.addAll(parseHistoryContextForOpenAi(historyContext));

        List<Map<String, Object>> userContent = new ArrayList<>();
        userContent.add(Map.of(
                "type", "text",
                "text", Objects.toString(message, "请看这张图")
        ));
        userContent.add(Map.of(
                "type", "image_url",
                "image_url", Map.of("url", imageUrl)
        ));

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);
        messages.add(userMessage);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);

        String responseBody = restClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractAssistantContent(responseBody);
    }

    private List<Map<String, Object>> parseHistoryContextForOpenAi(String historyContext) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (historyContext == null || historyContext.isEmpty()) {
            return messages;
        }
        try {
            JSONArray array = JSONUtil.parseArray(historyContext);
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String role = obj.getStr("role");
                String content = Objects.toString(obj.getStr("content"), "");
                messages.add(Map.of(
                        "role", "model".equals(role) ? "assistant" : "user",
                        "content", content
                ));
            }
        } catch (Exception e) {
            log.warn("解析多模态聊天历史失败: {}", e.getMessage());
        }
        return messages;
    }

    private String extractAssistantContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "抱歉，Nowl AI现在有点忙，请稍后再试~";
        }

        JSONObject response = JSONUtil.parseObj(responseBody);
        JSONArray choices = response.getJSONArray("choices");
        if (choices == null || choices.isEmpty()) {
            log.warn("多模态聊天未返回 choices: {}", responseBody);
            return "抱歉，Nowl AI现在有点忙，请稍后再试~";
        }

        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice == null ? null : firstChoice.getJSONObject("message");
        if (message == null) {
            log.warn("多模态聊天未返回 message: {}", responseBody);
            return "抱歉，Nowl AI现在有点忙，请稍后再试~";
        }

        Object content = message.get("content");
        if (content instanceof String text && !text.isBlank()) {
            return text;
        }
        if (content instanceof JSONArray contentArray) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < contentArray.size(); i++) {
                JSONObject part = contentArray.getJSONObject(i);
                if (part == null) {
                    continue;
                }
                String text = part.getStr("text");
                if (text != null && !text.isBlank()) {
                    if (builder.length() > 0) {
                        builder.append('\n');
                    }
                    builder.append(text);
                }
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }

        log.warn("多模态聊天返回内容无法解析: {}", responseBody);
        return "抱歉，我没有成功读取到图片内容，请稍后再试~";
    }

    private String resolveOpenAiApiBaseUrl() {
        String normalized = Objects.toString(baseUrl, "").trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized;
    }

    /**
     * 解析历史上下文JSON为Message列表
     */
    private List<Message> parseHistoryContext(String historyContext) {
        List<Message> messages = new ArrayList<>();
        if (historyContext == null || historyContext.isEmpty()) {
            return messages;
        }
        try {
            JSONArray array = JSONUtil.parseArray(historyContext);
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String role = obj.getStr("role");
                String content = obj.getStr("content");
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else {
                    messages.add(new AssistantMessage(content));
                }
            }
        } catch (Exception e) {
            log.warn("解析历史上下文失败: {}", e.getMessage());
        }
        return messages;
    }
}
