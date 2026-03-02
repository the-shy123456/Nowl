package com.unimarket.module.aiassistant.service.impl;

import com.unimarket.ai.vo.AiGoodsCardVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI 工具函数返回：用于把真实的查询结果交给模型，严禁编造 cards。
 *
 * 说明：
 * - 该类需要保持 JavaBean 形态（getter/setter），便于函数调用框架序列化。
 */
public class GoodsToolOutput {
    private String intent;
    private String keyword;
    private String scopeText;
    private boolean fallbackUsed;
    private long total;
    private List<AiGoodsCardVO> cards;
    private Integer limit;
    private Integer page;
    private BigDecimal maxPrice;
    private Boolean hasMore;

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getScopeText() {
        return scopeText;
    }

    public void setScopeText(String scopeText) {
        this.scopeText = scopeText;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<AiGoodsCardVO> getCards() {
        return cards;
    }

    public void setCards(List<AiGoodsCardVO> cards) {
        this.cards = cards;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Boolean getHasMore() {
        return hasMore;
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}

