package com.unimarket.module.risk.enums;

/**
 * 风控运行模式。
 */
public enum RiskMode {
    OFF,
    BASIC,
    FULL;

    public static RiskMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return FULL;
        }
        try {
            return RiskMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignore) {
            return FULL;
        }
    }
}
