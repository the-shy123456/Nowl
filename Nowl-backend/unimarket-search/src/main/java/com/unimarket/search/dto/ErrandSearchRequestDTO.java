package com.unimarket.search.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 跑腿搜索请求DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErrandSearchRequestDTO extends PageQuery {

    private String keyword;

    private Integer taskStatus;

    private String schoolCode;

    private String campusCode;

    private BigDecimal minReward;

    private BigDecimal maxReward;

    /**
     * 排序类型: 0-综合(默认) 1-最新 2-赏金升序 3-赏金降序
     */
    private Integer sortType;
}
