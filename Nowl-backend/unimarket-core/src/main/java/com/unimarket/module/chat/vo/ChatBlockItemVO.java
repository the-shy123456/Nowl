package com.unimarket.module.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatBlockItemVO {

    private Long userId;

    private String nickName;

    private String avatar;

    private LocalDateTime blockTime;
}

