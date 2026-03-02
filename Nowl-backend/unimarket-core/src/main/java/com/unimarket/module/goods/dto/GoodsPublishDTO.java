package com.unimarket.module.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

/**
 * 商品发布DTO
 */
@Data
public class GoodsPublishDTO {

    /**
     * 商品标题
     */
    @NotBlank(message = "商品标题不能为空")
    @Length(max = 100, message = "商品标题不能超过100个字符")
    private String title;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;

    /**
     * 商品价格
     */
    @NotNull(message = "商品价格不能为空")
    private BigDecimal price;

    /**
     * 商品原价
     */
    private BigDecimal originalPrice;

    /**
     * 商品详情
     */
    @Length(max = 2000, message = "商品详情不能超过2000个字符")
    private String description;

    /**
     * 封面图URL
     */
    private String image;

    /**
     * 详情图URL数组（JSON格式）
     */
    private String imageList;

    /**
     * 商品成色：1-10级（1全新，10破损严重）
     */
    @Range(min = 1, max = 10, message = "商品成色范围为1-10")
    private Integer itemCondition;

    /**
     * 交易方式：0-仅线下，1-仅邮寄，2-皆可
     */
    @Range(min = 0, max = 2, message = "交易方式范围为0-2")
    private Integer tradeType;

    /**
     * 运费
     */
    private BigDecimal deliveryFee;

    /**
     * 交易状态：0-在售，1-已售出，2-下架（用于重新上架时设置为0）
     */
    private Integer tradeStatus;
}
