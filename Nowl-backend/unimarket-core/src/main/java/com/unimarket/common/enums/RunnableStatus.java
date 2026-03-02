package com.unimarket.common.enums;

/**
 * 跑腿员认证状态
 */
public enum RunnableStatus {
    NOT_APPLIED(0, "未申请"),
    PENDING(1, "审核中"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已拒绝");

    private final Integer code;
    private final String desc;

    RunnableStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
