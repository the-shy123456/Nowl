package com.unimarket.module.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品详情VO
 */
@Data
public class GoodsDetailVO {

    /**
     * 商品ID
     */
    private Long productId;

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
     * 卖家信用分
     */
    private Integer sellerCreditScore;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 商品标题
     */
    private String title;

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
     * 商品详情
     */
    private String description;

    /**
     * 详情图URL数组（JSON格式）
     */
    private String imageList;

    /**
     * 封面图URL
     */
    private String image;

    /**
     * 商品成色
     */
    private Integer itemCondition;

    /**
     * 交易方式
     */
    private Integer tradeType;

    /**
     * 运费
     */
    private BigDecimal deliveryFee;

    /**
     * 审核状态
     */
    private Integer reviewStatus;

    /**
     * 审核原因
     */
    private String auditReason;

    /**
     * AI定价建议
     */
    private BigDecimal aiValuation;

    /**
     * 收藏数
     */
    private Integer collectCount;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品原价
     */
    private BigDecimal originalPrice;

    /**
     * 是否已收藏
     */
    private Boolean isCollected;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
