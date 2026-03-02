package com.unimarket.module.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体类
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long messageId;

    private Long senderId;

    private Long receiverId;

    private String schoolCode;

    private String campusCode;

    private String content;

    private Integer messageType; // 0-文本, 1-图片

    private Integer isRead; // 0-未读, 1-已读

    private String riskLevel;

    private LocalDateTime createTime;
}
