package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 纠纷目标类型枚举
 */
@Getter
@AllArgsConstructor
public enum DisputeTargetType {

    /**
     * 商品订单
     */
    ORDER(0, "商品交易"),

    /**
     * 跑腿任务
     */
    ERRAND(1, "跑腿劳务");

    private final Integer code;
    private final String description;

    public static DisputeTargetType getByCode(Integer code) {
        for (DisputeTargetType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
