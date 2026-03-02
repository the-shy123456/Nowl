package com.unimarket.module.risk.enums;

/**
 * 风控动作
 */
public enum RiskAction {

    ALLOW,
    REJECT,
    CHALLENGE,
    REVIEW,
    LIMIT;

    public static RiskAction from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALLOW;
        }
        for (RiskAction value : values()) {
            if (value.name().equalsIgnoreCase(raw.trim())) {
                return value;
            }
        }
        return ALLOW;
    }
}

