package com.unimarket.ai.vo;

import lombok.Data;

/**
 * 商品估价结果VO
 */
@Data
public class GoodsPriceEstimateVO {

    /**
     * 建议价格
     */
    private Double suggestedPrice;

    /**
     * 估价原因（简短说明）
     */
    private String reason;

    /**
     * 参考的同类商品数量
     */
    private Integer referenceCount;
}
