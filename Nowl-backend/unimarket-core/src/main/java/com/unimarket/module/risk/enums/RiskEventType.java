package com.unimarket.module.risk.enums;

/**
 * 风控事件类型
 */
public final class RiskEventType {

    private RiskEventType() {
    }

    public static final String LOGIN = "LOGIN";
    public static final String GOODS_PUBLISH = "GOODS_PUBLISH";
    public static final String ERRAND_PUBLISH = "ERRAND_PUBLISH";
    public static final String ERRAND_ACCEPT = "ERRAND_ACCEPT";
    public static final String CHAT_SEND = "CHAT_SEND";
    public static final String AI_CHAT_SEND = "AI_CHAT_SEND";
    public static final String FOLLOW_USER = "FOLLOW_USER";
}

