package com.unimarket.ai.service;

import org.springframework.ai.model.function.FunctionCallback;

import java.util.List;

/**
 * AI 对话能力接口（仅负责聊天与 Function Calling）
 */
public interface AiChatService {

    /**
     * 与小Q对话（纯AI对话，不保存历史）
     *
     * @param message 用户消息
     * @param imageUrl 图片URL（可选）
     * @param historyContext 历史对话上下文（JSON格式）
     * @return AI回复
     */
    String chat(String message, String imageUrl, String historyContext);

    /**
     * 与 AI 对话（支持 Function Calling）
     *
     * @param message 用户消息
     * @param historyContext 历史对话上下文（JSON格式）
     * @param systemPrompt 系统提示词
     * @param functionCallbacks 可用函数列表
     * @return AI回复
     */
    String chatWithFunctions(
            String message,
            String historyContext,
            String systemPrompt,
            List<FunctionCallback> functionCallbacks
    );
}
