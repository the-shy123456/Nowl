package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录轨迹视图
 */
@Data
public class LoginTraceVO {

    private Long id;

    private String traceId;

    private Long userId;

    private String phone;

    private String ip;

    private String deviceId;

    private String geo;

    private String loginResult;

    private String failReason;

    private String riskLevel;

    private LocalDateTime createTime;
}

