package com.unimarket.module.aiassistant.service;

import com.unimarket.ai.vo.AiChatMessageVO;

import java.util.List;

/**
 * AI聊天历史记录Service接口
 */
public interface AiChatHistoryService {

    /**
     * 保存用户消息
     * @param userId 用户ID
     * @param content 消息内容
     * @param imageUrl 图片URL
     * @param riskLevel 风险等级
     */
    void saveUserMessage(Long userId, String content, String imageUrl, String riskLevel);

    /**
     * 保存AI回复
     * @param userId 用户ID
     * @param content AI回复内容
     */
    void saveModelMessage(Long userId, String content);

    /**
     * 获取用户的聊天历史记录
     * @param userId 用户ID
     * @param limit 限制条数
     * @return 聊天记录列表
     */
    List<AiChatMessageVO> getHistory(Long userId, int limit);

    /**
     * 清除用户的聊天历史记录
     * @param userId 用户ID
     */
    void clearHistory(Long userId);

    /**
     * 获取最近的对话上下文（用于AI对话时携带）
     * @param userId 用户ID
     * @param limit 限制条数
     * @return JSON格式的历史上下文
     */
    String getRecentContext(Long userId, int limit);
}

