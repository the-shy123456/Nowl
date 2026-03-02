package com.unimarket.module.review.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价列表项VO
 */
@Data
public class ReviewListItemVO {

    /**
     * 评价ID
     */
    private Long reviewId;

    /**
     * 类型：0-商品交易，1-跑腿任务
     */
    private Integer targetType;

    /**
     * 类型描述
     */
    private String targetTypeDesc;

    /**
     * 订单ID（targetType=0 时有效）
     */
    private Long orderId;

    /**
     * 商品ID（targetType=0 时有效）
     */
    private Long productId;

    /**
     * 跑腿任务ID（targetType=1 时有效）
     */
    private Long taskId;

    /**
     * 评价人ID
     */
    private Long reviewerId;

    /**
     * 评价人昵称（匿名时显示"匿名用户"）
     */
    private String reviewerName;

    /**
     * 评价人头像
     */
    private String reviewerAvatar;

    /**
     * 评价人的交易身份（买家/卖家/跑腿员/发单者）
     */
    private String reviewerRole;

    /**
     * 被评价人的交易身份（买家/卖家/跑腿员/发单者）
     */
    private String reviewedRole;

    /**
     * 评分：1-5星
     */
    private Integer rating;

    /**
     * 评价内容
     */
    private String content;

    /**
     * 是否匿名
     */
    private Boolean anonymous;

    /**
     * 关联内容标题（商品名/跑腿标题）
     */
    private String contentTitle;

    /**
     * 关联内容图片
     */
    private String contentImage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
