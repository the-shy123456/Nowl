package com.unimarket.module.chat.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ContactVO {
    private Long userId;        // 对方用户ID
    private String nickName;    // 对方昵称
    private String avatar;      // 对方头像
    private String lastMessage; // 最后一条消息内容
    private LocalDateTime lastTime; // 最后一条消息时间
    private Integer unreadCount; // 未读消息数
}
