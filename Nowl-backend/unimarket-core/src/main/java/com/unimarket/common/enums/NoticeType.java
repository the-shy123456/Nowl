package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知类型枚举
 */
@Getter
@AllArgsConstructor
public enum NoticeType {

    /**
     * 系统通知
     */
    SYSTEM(0, "系统通知"),

    /**
     * 交易通知
     */
    TRADE(1, "交易通知"),

    /**
     * 评价通知
     */
    REVIEW(2, "评价通知"),

    /**
     * 纠纷通知
     */
    DISPUTE(3, "纠纷通知");

    private final Integer code;
    private final String description;

    public static NoticeType getByCode(Integer code) {
        for (NoticeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
