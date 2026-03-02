package com.unimarket.ai.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * AI 聊天中可展示的商品卡片
 */
@Data
public class AiGoodsCardVO {

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
     * 商品图片
     */
    private String image;

    /**
     * 卖家昵称
     */
    private String sellerName;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;
}
