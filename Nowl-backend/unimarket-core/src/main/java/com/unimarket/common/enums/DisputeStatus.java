package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 纠纷状态枚举
 */
@Getter
@AllArgsConstructor
public enum DisputeStatus {

    /**
     * 待处理
     */
    PENDING(0, "待处理"),

    /**
     * 处理中
     */
    PROCESSING(1, "处理中"),

    /**
     * 已解决
     */
    RESOLVED(2, "已解决"),

    /**
     * 已驳回
     */
    REJECTED(3, "已驳回"),

    /**
     * 已撤回
     */
    WITHDRAWN(4, "已撤回");

    private final Integer code;
    private final String description;

    public static DisputeStatus getByCode(Integer code) {
        for (DisputeStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

    public static String getDescriptionByCode(Integer code) {
        DisputeStatus status = getByCode(code);
        return status != null ? status.getDescription() : "未知";
    }
}
