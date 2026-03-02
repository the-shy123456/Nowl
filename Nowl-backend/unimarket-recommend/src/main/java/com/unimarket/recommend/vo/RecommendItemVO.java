package com.unimarket.recommend.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 推荐商品VO
 */
@Data
public class RecommendItemVO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 卖家昵称
     */
    private String sellerName;

    /**
     * 卖家头像
     */
    private String sellerAvatar;

    /**
     * 卖家认证状态
     */
    private Integer sellerAuthStatus;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 是否已收藏
     */
    private Boolean isCollected;

    /**
     * 推荐分数
     */
    private Double score;

    /**
     * 推荐类型: cf(协同过滤), content(内容推荐), hot(热门), hybrid(混合), fallback(兜底)
     */
    private String recommendType;
}
