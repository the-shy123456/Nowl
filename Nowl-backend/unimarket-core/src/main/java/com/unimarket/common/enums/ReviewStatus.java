package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品审核状态枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewStatus {

    /**
     * 待审核
     */
    WAIT_REVIEW(0, "待审核"),

    /**
     * AI审核通过
     */
    AI_PASSED(1, "AI审核通过"),

    /**
     * 人工审核通过
     */
    MANUAL_PASSED(2, "人工审核通过"),

    /**
     * 违规/审核不通过
     */
    REJECTED(3, "违规"),

    /**
     * 待人工复核
     */
    WAIT_MANUAL(4, "待人工复核");

    private final Integer code;
    private final String description;

    public static ReviewStatus getByCode(Integer code) {
        for (ReviewStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
