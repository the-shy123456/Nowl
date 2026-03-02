package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 跑腿任务状态枚举
 */
@Getter
@AllArgsConstructor
public enum ErrandStatus {

    /**
     * 待接单
     */
    PENDING(0, "待接单"),

    /**
     * 进行中（已接单）
     */
    IN_PROGRESS(1, "进行中"),

    /**
     * 待确认（已送达，等待发布者确认）
     */
    PENDING_CONFIRM(2, "待确认"),

    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),

    /**
     * 已取消
     */
    CANCELLED(4, "已取消");

    private final Integer code;
    private final String description;

    /**
     * 根据状态码获取枚举
     */
    public static ErrandStatus getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ErrandStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
