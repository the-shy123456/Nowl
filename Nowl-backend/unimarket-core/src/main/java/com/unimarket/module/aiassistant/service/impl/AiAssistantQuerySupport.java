package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;

import java.math.BigDecimal;

final class AiAssistantQuerySupport {

    static final int SEARCH_CARD_LIMIT = 4;
    static final int RECOMMEND_CARD_LIMIT = 3;
    static final int CHEAPEST_CARD_LIMIT = 1;
    static final int TOOL_CARD_MAX_LIMIT = 10;

    private AiAssistantQuerySupport() {
    }

    static int getDefaultLimit(QueryIntent intent) {
        return switch (intent) {
            case CHEAPEST -> CHEAPEST_CARD_LIMIT;
            case RECOMMEND -> RECOMMEND_CARD_LIMIT;
            case SEARCH, GENERAL -> SEARCH_CARD_LIMIT;
        };
    }

    static int resolveToolLimit(Integer limit, int defaultLimit) {
        if (limit == null || limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, TOOL_CARD_MAX_LIMIT);
    }

    static int resolveQueryPage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    static BigDecimal normalizeMaxPrice(BigDecimal maxPrice) {
        if (maxPrice == null || maxPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return maxPrice;
    }

    static QueryConstraints resolveConstraintsByIntent(
            QueryIntent intent,
            QueryConstraints base,
            String overrideKeyword
    ) {
        String keyword = AiAssistantTextSupport.cleanupKeyword(StrUtil.blankToDefault(overrideKeyword, base == null ? null : base.keyword));
        int defaultLimit = getDefaultLimit(intent);
        int limit = resolveToolLimit(base == null ? null : base.limit, defaultLimit);
        int page = resolveQueryPage(base == null ? null : base.page);
        BigDecimal maxPrice = normalizeMaxPrice(base == null ? null : base.maxPrice);

        if (intent == QueryIntent.CHEAPEST) {
            limit = CHEAPEST_CARD_LIMIT;
            page = 0;
        }

        return new QueryConstraints(keyword, limit, maxPrice, page);
    }

    static QueryConstraints resolveQueryConstraints(
            String message,
            QueryIntent intent,
            AiChatQueryContext queryContext,
            boolean switchBatchRequest
    ) {
        String contextKeyword = queryContext == null ? null : AiAssistantTextSupport.cleanupKeyword(queryContext.getKeyword());
        String extractedKeyword = AiAssistantTextSupport.extractKeyword(message, intent);
        String keyword = StrUtil.blankToDefault(contextKeyword, extractedKeyword);
        keyword = AiAssistantTextSupport.cleanupKeyword(keyword);

        Integer contextLimit = queryContext == null ? null : queryContext.getLimit();
        Integer requestedLimit = AiAssistantTextSupport.extractRequestedLimit(message, intent);
        int defaultLimit = getDefaultLimit(intent);
        int limit = resolveToolLimit(
                requestedLimit != null ? requestedLimit : contextLimit,
                defaultLimit
        );
        if (intent == QueryIntent.CHEAPEST) {
            limit = CHEAPEST_CARD_LIMIT;
        }

        BigDecimal contextMaxPrice = queryContext == null ? null : queryContext.getMaxPrice();
        BigDecimal requestedMaxPrice = AiAssistantTextSupport.extractMaxPrice(message);
        BigDecimal maxPrice = normalizeMaxPrice(requestedMaxPrice != null ? requestedMaxPrice : contextMaxPrice);

        int page = switchBatchRequest
                ? resolveQueryPage(queryContext == null ? null : queryContext.getPage())
                : 0;

        return new QueryConstraints(keyword, limit, maxPrice, page);
    }

    static void applyQueryMetadata(AiChatResponseVO response, QuerySnapshot snapshot, QueryConstraints constraints) {
        if (response == null || constraints == null) {
            return;
        }
        response.setQueryLimit(constraints.limit);
        response.setQueryPage(constraints.page);
        response.setMaxPrice(constraints.maxPrice);
        response.setTotal(snapshot == null ? 0L : snapshot.total);

        long total = snapshot == null ? 0L : snapshot.total;
        long shown = (long) (constraints.page + 1) * constraints.limit;
        response.setHasMore(total > shown);
    }

    static String buildScopeText(boolean fallbackUsed, String schoolCode, String campusCode) {
        if (fallbackUsed) {
            return "你所在范围暂无结果，已扩展到全平台检索";
        }
        if (StrUtil.isNotBlank(campusCode)) {
            return "已按你当前校区检索";
        }
        if (StrUtil.isNotBlank(schoolCode)) {
            return "已按你当前学校检索";
        }
        return "已按全平台检索";
    }

    static String formatPrice(BigDecimal price) {
        if (price == null) {
            return "未知";
        }
        return price.stripTrailingZeros().toPlainString();
    }
}

