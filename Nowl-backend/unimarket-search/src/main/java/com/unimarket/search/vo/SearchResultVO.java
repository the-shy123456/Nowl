package com.unimarket.search.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 搜索结果VO
 */
@Data
public class SearchResultVO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品标题（可能包含高亮标签）
     */
    private String title;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

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
     * 学校编码
     */
    private String schoolCode;

    /**
     * 学校名称
     */
    private String schoolName;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 交易状态
     */
    private Integer tradeStatus;

    /**
     * 商品图片
     */
    private String image;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 浏览数
     */
    private Integer viewCount;

    /**
     * 热度分
     */
    private Double hotScore;

    /**
     * 搜索匹配分数
     */
    private Float score;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 是否已收藏（需要根据用户查询后填充）
     */
    private Boolean isCollected;
}
