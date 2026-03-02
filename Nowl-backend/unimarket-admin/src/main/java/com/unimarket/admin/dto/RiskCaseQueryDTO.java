package com.unimarket.admin.dto;

import com.unimarket.common.result.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控工单查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskCaseQueryDTO extends PageQuery {

    private String caseStatus;

    private Long assigneeId;

    private String schoolCode;

    private String campusCode;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
