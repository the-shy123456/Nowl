package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评价目标类型
 */
@Getter
@AllArgsConstructor
public enum ReviewTargetType {

    /**
     * 商品交易
     */
    GOODS_TRADE(0, "商品交易"),

    /**
     * 跑腿任务
     */
    ERRAND_SERVICE(1, "跑腿任务");

    private final Integer code;
    private final String description;

    public static ReviewTargetType getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewTargetType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
