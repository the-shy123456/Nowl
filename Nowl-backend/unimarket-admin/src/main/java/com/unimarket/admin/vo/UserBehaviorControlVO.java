package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为管控视图
 */
@Data
public class UserBehaviorControlVO {

    private Long id;

    private Long userId;

    private String userName;

    private String schoolCode;

    private String campusCode;

    private String eventType;

    private String controlAction;

    private String reason;

    private LocalDateTime expireTime;

    private Integer status;

    private Long operatorId;

    private LocalDateTime createTime;
}

