package com.unimarket.module.risk.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * 风控评估上下文
 */
@Data
@Builder
public class RiskContext {

    /**
     * 事件类型，例如 LOGIN / GOODS_PUBLISH
     */
    private String eventType;

    /**
     * 主体类型，默认 USER
     */
    private String subjectType;

    /**
     * 主体标识，默认 userId
     */
    private String subjectId;

    private Long userId;

    private String schoolCode;

    private String campusCode;

    private String requestIp;

    private String deviceId;

    /**
     * 风险特征(轻量字段)
     */
    private Map<String, Object> features;

    /**
     * 原始载荷(全量上下文)
     */
    private Map<String, Object> rawPayload;
}

