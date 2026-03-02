package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台操作审计视图
 */
@Data
public class AdminOperationAuditVO {

    private Long id;

    private String traceId;

    private Long operatorId;

    private String operatorIp;

    private String module;

    private String action;

    private String targetType;

    private String targetId;

    private String requestPayload;

    private String resultStatus;

    private String resultMessage;

    private Integer costMs;

    private LocalDateTime createTime;
}

