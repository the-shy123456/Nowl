package com.unimarket.module.goods.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品信息实体类
 */
@Data
@TableName("goods_info")
public class GoodsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 商品ID（主键）
     */
    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 商品标题
     */
    private String title;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 交易状态：0-在售，1-已售出，2-下架
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
     * 商品成色：1-10级（1全新，10破损严重）
     */
    private Integer itemCondition;

    /**
     * 交易方式：0-仅线下，1-仅邮寄，2-皆可
     */
    private Integer tradeType;

    /**
     * 运费
     */
    private BigDecimal deliveryFee;

    /**
     * 审核状态：0-待审核，1-AI审核通过，2-人工复核通过，3-违规驳回，4-待人工复核
     */
    private Integer reviewStatus;

    /**
     * 审核不通过原因
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
     * 商品原价（砍价参考）
     */
    private BigDecimal originalPrice;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 软删除
     */
    @TableLogic
    private Integer isDeleted;
}
