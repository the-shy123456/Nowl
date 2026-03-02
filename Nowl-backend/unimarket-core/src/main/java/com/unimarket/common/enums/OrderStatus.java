package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单状态枚举
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    /**
     * 待支付
     */
    PENDING_PAYMENT(0, "待支付"),

    /**
     * 待发货
     */
    PENDING_DELIVERY(1, "待发货"),

    /**
     * 待收货
     */
    PENDING_RECEIVE(2, "待收货"),

    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(4, "已取消"),

    /**
     * 已结束（纠纷完结等非正常完成）
     */
    ENDED(5, "已结束");

    private final Integer code;
    private final String description;

    public static OrderStatus getByCode(Integer code) {
        for (OrderStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
