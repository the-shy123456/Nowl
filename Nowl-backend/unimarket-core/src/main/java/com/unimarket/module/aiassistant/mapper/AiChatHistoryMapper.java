package com.unimarket.module.aiassistant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.aiassistant.entity.AiChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI聊天历史记录Mapper
 */
@Mapper
public interface AiChatHistoryMapper extends BaseMapper<AiChatHistory> {
}

