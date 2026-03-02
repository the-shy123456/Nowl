package com.unimarket.module.aiassistant.service.impl;

import java.math.BigDecimal;

/**
 * AI 工具函数入参：由模型发起函数调用时传入。
 *
 * 说明：
 * - 该类需要保持 JavaBean 形态（getter/setter），便于函数调用框架反序列化。
 */
public class GoodsToolInput {
    private String keyword;
    private Integer limit;
    private BigDecimal maxPrice;
    private Integer page;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }
}

