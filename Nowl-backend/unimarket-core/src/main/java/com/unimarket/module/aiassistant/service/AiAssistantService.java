package com.unimarket.module.aiassistant.service;

import com.unimarket.ai.vo.AiChatMessageVO;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;

import java.util.List;

/**
 * AI助手业务编排服务（仅聊天能力）
 */
public interface AiAssistantService {

    /**
     * 与小Q对话（含风控与历史记录）
     */
    AiChatResponseVO chat(
            Long userId,
            String schoolCode,
            String campusCode,
            String message,
            String imageUrl,
            AiChatQueryContext queryContext
    );

    /**
     * 获取聊天历史
     */
    List<AiChatMessageVO> getHistory(Long userId, int limit);

    /**
     * 清理聊天历史
     */
    void clearHistory(Long userId);
}

