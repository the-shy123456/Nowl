package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 认证状态枚举
 */
@Getter
@AllArgsConstructor
public enum AuthStatus {

    /**
     * 未认证
     */
    NOT_AUTHENTICATED(0, "未认证"),

    /**
     * 待审核
     */
    PENDING(1, "待审核"),

    /**
     * 已通过
     */
    APPROVED(2, "已通过"),

    /**
     * 已拒绝
     */
    REJECTED(3, "已拒绝");

    private final Integer code;
    private final String description;

    /**
     * 根据code获取枚举
     */
    public static AuthStatus getByCode(Integer code) {
        for (AuthStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
