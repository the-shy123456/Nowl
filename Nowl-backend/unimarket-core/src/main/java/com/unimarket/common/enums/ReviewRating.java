package com.unimarket.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评分等级枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewRating {

    VERY_BAD(1, "非常差", -5),
    BAD(2, "不满意", -3),
    NORMAL(3, "一般", 0),
    GOOD(4, "满意", 1),
    VERY_GOOD(5, "非常满意", 3);

    private final Integer code;
    private final String description;
    private final Integer creditChange;

    public static ReviewRating getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ReviewRating rating : values()) {
            if (rating.getCode().equals(code)) {
                return rating;
            }
        }
        return null;
    }

    /**
     * 获取评分对应的信用分变化
     */
    public static Integer getCreditChangeByCode(Integer code) {
        ReviewRating rating = getByCode(code);
        return rating != null ? rating.getCreditChange() : 0;
    }

    /**
     * 是否为差评（需要填写理由）
     */
    public static boolean isBadReview(Integer code) {
        return code != null && code <= 2;
    }
}
