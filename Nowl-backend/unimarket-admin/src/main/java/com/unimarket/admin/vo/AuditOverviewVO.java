package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审计总览
 */
@Data
public class AuditOverviewVO {

    private Integer windowDays;

    private Long totalOperations;

    private Long failedOperations;

    private Long permissionChanges;

    private Long loginAttempts;

    private Long loginFailures;

    private Long highRiskLoginCount;

    private LocalDateTime lastOperationTime;
}
