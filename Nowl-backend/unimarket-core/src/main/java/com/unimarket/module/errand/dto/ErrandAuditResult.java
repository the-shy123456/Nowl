package com.unimarket.module.errand.dto;

import com.unimarket.common.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 跑腿任务审核结果
 */
@Data
@AllArgsConstructor
public class ErrandAuditResult {

    /**
     * 审核状态，取值见 {@link ReviewStatus}
     */
    private Integer reviewStatus;

    /**
     * 审核原因（可为空）
     */
    private String reason;

    public boolean isRejected() {
        return ReviewStatus.REJECTED.getCode().equals(reviewStatus);
    }

    public boolean isWaitManual() {
        return ReviewStatus.WAIT_MANUAL.getCode().equals(reviewStatus);
    }

    public boolean isPassed() {
        return ReviewStatus.AI_PASSED.getCode().equals(reviewStatus)
            || ReviewStatus.MANUAL_PASSED.getCode().equals(reviewStatus);
    }
}
