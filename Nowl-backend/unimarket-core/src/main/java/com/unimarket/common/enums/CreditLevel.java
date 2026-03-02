package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 信用等级枚举
 */
@Getter
@AllArgsConstructor
public enum CreditLevel {

    EXCELLENT(120, 150, "优秀", "gold"),
    GOOD(100, 119, "良好", "green"),
    NORMAL(80, 99, "一般", "blue"),
    POOR(60, 79, "较差", "orange"),
    DANGER(0, 59, "危险", "red");

    private final Integer minScore;
    private final Integer maxScore;
    private final String description;
    private final String color;

    /**
     * 根据信用分获取等级
     */
    public static CreditLevel getByScore(Integer score) {
        if (score == null) {
            return NORMAL;
        }
        for (CreditLevel level : values()) {
            if (score >= level.getMinScore() && score <= level.getMaxScore()) {
                return level;
            }
        }
        if (score > 150) {
            return EXCELLENT;
        }
        return DANGER;
    }

    /**
     * 获取等级描述
     */
    public static String getDescriptionByScore(Integer score) {
        return getByScore(score).getDescription();
    }

    /**
     * 获取等级颜色
     */
    public static String getColorByScore(Integer score) {
        return getByScore(score).getColor();
    }

    /**
     * 是否可以正常使用（信用分>=60）
     */
    public static boolean canNormalUse(Integer score) {
        return score != null && score >= 60;
    }

    /**
     * 是否被限制（信用分<80）
     */
    public static boolean isRestricted(Integer score) {
        return score != null && score < 80;
    }
}
