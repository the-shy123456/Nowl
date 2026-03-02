package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控工单视图
 */
@Data
public class RiskCaseVO {

    private Long caseId;

    private Long eventId;

    private Long decisionId;

    private String schoolCode;

    private String campusCode;

    private String caseStatus;

    private Long assigneeId;

    private String result;

    private String resultReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String eventType;

    private String subjectType;

    private String subjectId;

    private String decisionAction;

    private String riskLevel;
}

