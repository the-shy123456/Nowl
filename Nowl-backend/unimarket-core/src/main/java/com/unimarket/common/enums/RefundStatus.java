package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单退款状态枚举
 */
@Getter
@AllArgsConstructor
public enum RefundStatus {

    /**
     * 无退款
     */
    NONE(0, "无退款"),

    /**
     * 待处理
     */
    PENDING(1, "待处理"),

    /**
     * 已退款
     */
    APPROVED(2, "已退款"),

    /**
     * 已拒绝
     */
    REJECTED(3, "已拒绝");

    private final Integer code;
    private final String description;

    public static RefundStatus getByCode(Integer code) {
        for (RefundStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

