package com.unimarket.module.risk.enums;

/**
 * 风险等级
 */
public enum RiskLevel {

    LOW("low"),
    MEDIUM("medium"),
    HIGH("high");

    private final String code;

    RiskLevel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static RiskLevel fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return LOW;
        }
        for (RiskLevel value : values()) {
            if (value.code.equalsIgnoreCase(raw.trim())) {
                return value;
            }
        }
        return LOW;
    }
}

