package com.unimarket.module.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息返回对象
 */
@Data
public class ChatMessageVO {

    private Long senderId;

    private Long receiverId;

    private String content;

    private LocalDateTime createTime;
}
