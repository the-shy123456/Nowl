package com.unimarket.module.aiassistant.service.impl;

import java.math.BigDecimal;

final class QueryConstraints {
    final String keyword;
    final int limit;
    final BigDecimal maxPrice;
    final int page;

    QueryConstraints(String keyword, int limit, BigDecimal maxPrice, int page) {
        this.keyword = keyword;
        this.limit = limit;
        this.maxPrice = maxPrice;
        this.page = Math.max(page, 0);
    }
}

