package com.unimarket.module.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品列表VO
 */
@Data
public class GoodsVO {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品原价
     */
    private BigDecimal originalPrice;

    /**
     * 封面图URL
     */
    private String image;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 商品成色
     */
    private Integer itemCondition;

    /**
     * 交易状态
     */
    private Integer tradeStatus;

    /**
     * 交易方式
     */
    private Integer tradeType;

    /**
     * 审核状态
     */
    private Integer reviewStatus;

    /**
     * 审核原因
     */
    private String auditReason;

    /**
     * 收藏数
     */
    private Integer collectCount;

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
     * 学校名称
     */
    private String schoolName;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 是否已收藏
     */
    private Boolean isCollected;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
