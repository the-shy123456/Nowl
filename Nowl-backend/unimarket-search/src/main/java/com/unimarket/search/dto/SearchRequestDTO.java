package com.unimarket.search.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 搜索请求DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SearchRequestDTO extends PageQuery {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 分类ID（二级分类，精确匹配）
     */
    private Integer categoryId;

    /**
     * 分类ID列表（用于一级分类查询其下所有子分类）
     */
    private List<Integer> categoryIds;

    /**
     * 最低价格
     */
    private BigDecimal minPrice;

    /**
     * 最高价格
     */
    private BigDecimal maxPrice;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 交易状态: 0-在售 1-已售 2-下架
     */
    private Integer tradeStatus;

    /**
     * 卖家ID
     */
    private Long sellerId;

    /**
     * 排序类型: 0-综合(默认) 1-最新 2-价格升序 3-价格降序 4-热度
     */
    private Integer sortType;
}
