package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控事件视图
 */
@Data
public class RiskEventVO {

    private Long eventId;

    private String traceId;

    private String eventType;

    private String subjectType;

    private String subjectId;

    private String schoolCode;

    private String campusCode;

    private String riskFeatures;

    private String rawPayload;

    private LocalDateTime eventTime;

    private Long decisionId;

    private String decisionAction;

    private String riskLevel;

    private Double riskScore;

    private String matchedRuleCodes;

    private String decisionReason;
}

