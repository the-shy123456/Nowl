package com.unimarket.module.aiassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI聊天历史记录实体类
 */
@Data
@TableName("ai_chat_history")
public class AiChatHistory {

    @TableId(type = IdType.AUTO)
    private Long messageId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 角色：user-用户, model-AI
     */
    private String role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}

