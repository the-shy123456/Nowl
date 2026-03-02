package com.unimarket.module.goods.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 商品查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GoodsQueryDTO extends PageQuery {

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 一级分类ID（查询该一级分类下全部子分类）
     */
    private Integer parentCategoryId;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 关键词搜索
     */
    private String keyword;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 交易状态：0-在售，1-已售出，2-下架，3-软删除
     */
    private Integer tradeStatus;

    /**
     * 卖家ID（查询我的商品时使用）
     */
    private Long sellerId;

    /**
     * 排序方式：0-最新，1-价格升序，2-价格降序，3-热度（收藏数）
     */
    private Integer sortType;
}
