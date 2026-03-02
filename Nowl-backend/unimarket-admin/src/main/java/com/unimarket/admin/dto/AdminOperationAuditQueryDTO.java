package com.unimarket.admin.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 后台操作审计查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminOperationAuditQueryDTO extends PageQuery {

    private Long operatorId;

    private String module;

    private String action;

    private String resultStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}

