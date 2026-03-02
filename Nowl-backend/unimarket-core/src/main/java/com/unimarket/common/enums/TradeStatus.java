package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 商品交易状态枚举
 */
@Getter
@AllArgsConstructor
public enum TradeStatus {

    /**
     * 在售
     */
    ON_SALE(0, "在售"),

    /**
     * 已售出
     */
    SOLD(1, "已售出"),

    /**
     * 已下架
     */
    OFF_SHELF(2, "已下架");

    private final Integer code;
    private final String description;

    public static TradeStatus getByCode(Integer code) {
        for (TradeStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
