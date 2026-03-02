package com.unimarket.ai.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unimarket.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Media;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * AI 对话能力实现（仅负责聊天与 Function Calling）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;

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
            List<Message> historyMessages = parseHistoryContext(historyContext);

            UserMessage userMessage;
            if (imageUrl != null && !imageUrl.isEmpty()) {
                log.info("Nowl AI收到图片消息: {}", imageUrl);
                MimeType mimeType = resolveImageMimeType(imageUrl);
                userMessage = new UserMessage(message, List.of(new Media(mimeType, new URI(imageUrl))));
            } else {
                userMessage = new UserMessage(message);
            }

            List<Message> promptMessages = new ArrayList<>();
            promptMessages.add(new SystemMessage(SYSTEM_PROMPT_TEXT));
            promptMessages.addAll(historyMessages);
            promptMessages.add(userMessage);

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

    private MimeType resolveImageMimeType(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return MimeTypeUtils.IMAGE_JPEG;
        }

        String normalized = imageUrl.toLowerCase(Locale.ROOT);
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }

        if (normalized.endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        if (normalized.endsWith(".jpg") || normalized.endsWith(".jpeg")) {
            return MimeTypeUtils.IMAGE_JPEG;
        }
        if (normalized.endsWith(".gif")) {
            return MimeTypeUtils.IMAGE_GIF;
        }
        if (normalized.endsWith(".webp")) {
            return MimeTypeUtils.parseMimeType("image/webp");
        }
        if (normalized.endsWith(".bmp")) {
            return MimeTypeUtils.parseMimeType("image/bmp");
        }
        return MimeTypeUtils.IMAGE_JPEG;
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
