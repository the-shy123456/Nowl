package com.unimarket.admin.dto;

import com.unimarket.common.result.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控事件查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskEventQueryDTO extends PageQuery {

    private String eventType;

    private String subjectType;

    private String subjectId;

    private String decisionAction;

    private String riskLevel;

    private String schoolCode;

    private String campusCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
