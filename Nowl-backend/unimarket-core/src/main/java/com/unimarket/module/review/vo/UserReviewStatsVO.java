package com.unimarket.module.review.vo;

import lombok.Data;

/**
 * 用户评价统计VO
 */
@Data
public class UserReviewStatsVO {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 平均评分
     */
    private Double averageRating;

    /**
     * 总评价数
     */
    private Long totalReviews;

    /**
     * 好评数（4-5星）
     */
    private Long goodReviews;

    /**
     * 好评率（百分比）
     */
    private Double goodRate;

    /**
     * 信用分
     */
    private Integer creditScore;

    /**
     * 信用等级
     */
    private String creditLevel;

    /**
     * 信用等级颜色
     */
    private String creditColor;
}
