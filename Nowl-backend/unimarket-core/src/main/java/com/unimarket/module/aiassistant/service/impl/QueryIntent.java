package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;

import java.util.Locale;

enum QueryIntent {
    GENERAL,
    SEARCH,
    CHEAPEST,
    RECOMMEND;

    String toCode() {
        return name().toLowerCase(Locale.ROOT);
    }

    static QueryIntent parseCode(String intentCode) {
        if (StrUtil.isBlank(intentCode)) {
            return null;
        }
        String normalized = intentCode.toLowerCase(Locale.ROOT).trim();
        return switch (normalized) {
            case "general" -> GENERAL;
            case "search" -> SEARCH;
            case "cheapest" -> CHEAPEST;
            case "recommend" -> RECOMMEND;
            default -> null;
        };
    }
}

