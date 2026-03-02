package com.unimarket.module.aiassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unimarket.ai.vo.AiChatMessageVO;
import com.unimarket.module.aiassistant.entity.AiChatHistory;
import com.unimarket.module.aiassistant.mapper.AiChatHistoryMapper;
import com.unimarket.module.aiassistant.service.AiChatHistoryService;
import com.unimarket.module.aiassistant.util.AiChatContentCodec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI聊天历史记录Service实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatHistoryServiceImpl implements AiChatHistoryService {

    private final AiChatHistoryMapper aiChatHistoryMapper;
    private final ObjectMapper objectMapper;
    private static final int MAX_HISTORY_LIMIT = 100;

    @Override
    public void saveUserMessage(Long userId, String content, String imageUrl, String riskLevel) {
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setRole("user");
        history.setContent(content);
        history.setImageUrl(imageUrl);
        history.setRiskLevel(riskLevel);
        history.setCreateTime(LocalDateTime.now());
        aiChatHistoryMapper.insert(history);
    }

    @Override
    public void saveModelMessage(Long userId, String content) {
        AiChatHistory history = new AiChatHistory();
        history.setUserId(userId);
        history.setRole("model");
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        aiChatHistoryMapper.insert(history);
    }

    @Override
    public List<AiChatMessageVO> getHistory(Long userId, int limit) {
        int safeLimit = clamp(limit, 1, MAX_HISTORY_LIMIT);
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .orderByDesc(AiChatHistory::getCreateTime)
                .last("LIMIT " + safeLimit);

        List<AiChatHistory> historyList = aiChatHistoryMapper.selectList(wrapper);

        // 倒序查询后反转为正序
        Collections.reverse(historyList);

        return historyList.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public void clearHistory(Long userId) {
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId);
        aiChatHistoryMapper.delete(wrapper);
    }

    @Override
    public String getRecentContext(Long userId, int limit) {
        int safeLimit = clamp(limit, 1, MAX_HISTORY_LIMIT);
        LambdaQueryWrapper<AiChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiChatHistory::getUserId, userId)
                .orderByDesc(AiChatHistory::getCreateTime)
                .last("LIMIT " + safeLimit);

        List<AiChatHistory> historyList = aiChatHistoryMapper.selectList(wrapper);

        if (historyList.isEmpty()) {
            return null;
        }

        // 倒序查询后反转为正序
        Collections.reverse(historyList);

        // 构建上下文
        List<Object> context = new ArrayList<>();
        for (AiChatHistory history : historyList) {
            String content = history.getContent();
            if ("model".equals(history.getRole())) {
                content = AiChatContentCodec.decodeModelContent(content).getReplyText();
            }
            context.add(new ContextMessage(history.getRole(), content));
        }

        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception e) {
            log.error("序列化聊天上下文失败", e);
            return null;
        }
    }

    private AiChatMessageVO convertToVO(AiChatHistory history) {
        AiChatMessageVO vo = new AiChatMessageVO();
        vo.setId(history.getMessageId());
        vo.setUserId(history.getUserId());
        vo.setRole(history.getRole());
        if ("model".equals(history.getRole())) {
            AiChatContentCodec.DecodedContent decoded = AiChatContentCodec.decodeModelContent(history.getContent());
            vo.setContent(decoded.getReplyText());
            vo.setCards(decoded.getCards());
        } else {
            vo.setContent(history.getContent());
        }
        vo.setImageUrl(history.getImageUrl());
        vo.setCreateTime(history.getCreateTime());
        return vo;
    }

    /**
     * 上下文消息结构
     */
    private static class ContextMessage {
        public String role;
        public String content;

        public ContextMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}

