package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 风控工单处理参数
 */
@Data
public class RiskCaseHandleDTO {

    @NotNull(message = "工单ID不能为空")
    private Long caseId;

    /**
     * CLOSED / REJECTED / PROCESSING
     */
    @NotBlank(message = "工单状态不能为空")
    private String caseStatus;

    /**
     * PASS / BLOCK / WARN
     */
    private String result;

    private String resultReason;
}

