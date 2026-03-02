package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.ai.vo.AiGoodsCardVO;
import com.unimarket.module.aiassistant.service.AiGoodsQueryService;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 商品查询编排：统一封装“按范围查询 + 兜底全平台”与文案拼装。
 */
@RequiredArgsConstructor
final class AiAssistantGoodsQueryEngine {

    private final AiGoodsQueryService aiGoodsQueryService;

    AiChatResponseVO ensureResolvedQueryResponse(
            AiChatResponseVO response,
            QueryIntent requestIntent,
            String schoolCode,
            String campusCode,
            QueryConstraints constraints
    ) {
        if (response == null) {
            return null;
        }

        QueryIntent responseIntent = QueryIntent.parseCode(response.getIntent());
        QueryIntent effectiveIntent = responseIntent == null ? requestIntent : responseIntent;
        if (effectiveIntent == null) {
            effectiveIntent = QueryIntent.GENERAL;
        }

        String replyText = StrUtil.blankToDefault(response.getReplyText(), "");
        String normalizedReply = AiAssistantTextSupport.normalizeMessage(replyText);
        boolean isPlaceholderReply = AiAssistantTextSupport.containsAny(normalizedReply,
                "正在为您查找", "正在查找", "正在查询", "稍后", "马上", "请稍等");

        String resolvedKeyword = AiAssistantTextSupport.cleanupKeyword(StrUtil.blankToDefault(response.getKeyword(), constraints.keyword));
        QueryConstraints resolvedConstraints = AiAssistantQuerySupport.resolveConstraintsByIntent(effectiveIntent, constraints, resolvedKeyword);
        List<AiGoodsCardVO> cards = response.getCards() == null ? Collections.emptyList() : response.getCards();

        if (effectiveIntent != QueryIntent.GENERAL) {
            if (cards.isEmpty() || isPlaceholderReply) {
                return handleGoodsQuery(effectiveIntent, schoolCode, campusCode, resolvedConstraints);
            }

            QuerySnapshot snapshot = queryWithFallback(
                    effectiveIntent,
                    schoolCode,
                    campusCode,
                    resolvedConstraints.keyword,
                    resolvedConstraints.limit,
                    resolvedConstraints.maxPrice,
                    resolvedConstraints.page
            );
            response.setCards(snapshot.cards);
            AiAssistantQuerySupport.applyQueryMetadata(response, snapshot, resolvedConstraints);
        }

        response.setIntent(effectiveIntent.toCode());
        response.setKeyword(resolvedKeyword);
        return response;
    }

    AiChatResponseVO handleGoodsQuery(
            QueryIntent intent,
            String schoolCode,
            String campusCode,
            QueryConstraints constraints
    ) {
        return switch (intent) {
            case CHEAPEST -> buildCheapestResponse(schoolCode, campusCode, constraints);
            case RECOMMEND -> buildRecommendResponse(schoolCode, campusCode, constraints);
            case SEARCH, GENERAL -> buildSearchResponse(schoolCode, campusCode, constraints);
        };
    }

    private AiChatResponseVO buildSearchResponse(String schoolCode, String campusCode, QueryConstraints constraints) {
        QueryConstraints resolved = AiAssistantQuerySupport.resolveConstraintsByIntent(QueryIntent.SEARCH, constraints, constraints.keyword);
        QuerySnapshot snapshot = queryWithFallback(
                QueryIntent.SEARCH,
                schoolCode,
                campusCode,
                resolved.keyword,
                resolved.limit,
                resolved.maxPrice,
                resolved.page
        );

        AiChatResponseVO response = new AiChatResponseVO();
        response.setCards(snapshot.cards);
        response.setIntent(QueryIntent.SEARCH.toCode());
        response.setKeyword(resolved.keyword);
        AiAssistantQuerySupport.applyQueryMetadata(response, snapshot, resolved);

        String subject = StrUtil.blankToDefault(resolved.keyword, "相关商品");
        if (snapshot.total <= 0) {
            response.setReplyText(String.format(
                    "我查了在售商品，暂时没有找到「%s」相关结果。你可以换个关键词再试试。",
                    subject
            ));
            return response;
        }

        String scopeText = AiAssistantQuerySupport.buildScopeText(snapshot.fallbackUsed, schoolCode, campusCode);
        String priceLimitText = resolved.maxPrice == null
                ? ""
                : String.format("（价格不高于¥%s）", AiAssistantQuerySupport.formatPrice(resolved.maxPrice));
        response.setReplyText(String.format("%s，找到 %d 条「%s」在售商品%s，先给你展示第 %d 批共 %d 条。",
                scopeText,
                snapshot.total,
                subject,
                priceLimitText,
                resolved.page + 1,
                snapshot.cards.size()));
        return response;
    }

    private AiChatResponseVO buildCheapestResponse(String schoolCode, String campusCode, QueryConstraints constraints) {
        QueryConstraints resolved = AiAssistantQuerySupport.resolveConstraintsByIntent(QueryIntent.CHEAPEST, constraints, constraints.keyword);
        QuerySnapshot snapshot = queryWithFallback(
                QueryIntent.CHEAPEST,
                schoolCode,
                campusCode,
                resolved.keyword,
                resolved.limit,
                resolved.maxPrice,
                resolved.page
        );

        AiChatResponseVO response = new AiChatResponseVO();
        response.setCards(snapshot.cards);
        response.setIntent(QueryIntent.CHEAPEST.toCode());
        response.setKeyword(resolved.keyword);
        AiAssistantQuerySupport.applyQueryMetadata(response, snapshot, resolved);

        String subject = StrUtil.blankToDefault(resolved.keyword, "商品");
        if (snapshot.cards.isEmpty()) {
            response.setReplyText(String.format(
                    "目前没查到「%s」在售商品，暂时无法给出最低价。你可以换个关键词再试试。",
                    subject
            ));
            return response;
        }

        AiGoodsCardVO cheapest = snapshot.cards.get(0);
        String priceText = AiAssistantQuerySupport.formatPrice(cheapest.getPrice());
        String scopeText = AiAssistantQuerySupport.buildScopeText(snapshot.fallbackUsed, schoolCode, campusCode);
        String suffix = snapshot.total > 1 ? String.format(Locale.ROOT, " 同类共 %d 条在售。", snapshot.total) : "";
        response.setReplyText(String.format(
                "%s，当前「%s」最低价是 ¥%s。%s",
                scopeText,
                subject,
                priceText,
                suffix
        ).trim());
        return response;
    }

    private AiChatResponseVO buildRecommendResponse(String schoolCode, String campusCode, QueryConstraints constraints) {
        QueryConstraints resolved = AiAssistantQuerySupport.resolveConstraintsByIntent(QueryIntent.RECOMMEND, constraints, constraints.keyword);
        QuerySnapshot snapshot = queryWithFallback(
                QueryIntent.RECOMMEND,
                schoolCode,
                campusCode,
                resolved.keyword,
                resolved.limit,
                resolved.maxPrice,
                resolved.page
        );

        AiChatResponseVO response = new AiChatResponseVO();
        response.setCards(snapshot.cards);
        response.setIntent(QueryIntent.RECOMMEND.toCode());
        response.setKeyword(resolved.keyword);
        AiAssistantQuerySupport.applyQueryMetadata(response, snapshot, resolved);

        if (snapshot.cards.isEmpty()) {
            response.setReplyText("当前没有可推荐的在售商品，你可以告诉我更具体的关键词。");
            return response;
        }

        String scopeText = AiAssistantQuerySupport.buildScopeText(snapshot.fallbackUsed, schoolCode, campusCode);
        String priceLimitText = resolved.maxPrice == null
                ? ""
                : String.format("，预算不高于 ¥%s", AiAssistantQuerySupport.formatPrice(resolved.maxPrice));
        if (StrUtil.isBlank(resolved.keyword)) {
            response.setReplyText(String.format(
                    "%s，给你挑了第 %d 批共 %d 件当前在售且热度较高的商品%s。",
                    scopeText,
                    resolved.page + 1,
                    snapshot.cards.size(),
                    priceLimitText
            ));
        } else {
            response.setReplyText(String.format(
                    "%s，基于「%s」给你推荐了第 %d 批共 %d 件在售商品%s。",
                    scopeText,
                    resolved.keyword,
                    resolved.page + 1,
                    snapshot.cards.size(),
                    priceLimitText
            ));
        }
        return response;
    }

    QuerySnapshot queryWithFallback(
            QueryIntent intent,
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    ) {
        List<AiGoodsCardVO> scopedCards = queryCards(intent, schoolCode, campusCode, keyword, limit, maxPrice, page);
        long scopedTotal = aiGoodsQueryService.countAvailableGoodsByKeyword(schoolCode, campusCode, keyword, maxPrice);

        if (!scopedCards.isEmpty() || (StrUtil.isBlank(schoolCode) && StrUtil.isBlank(campusCode))) {
            return new QuerySnapshot(scopedCards, scopedTotal, false);
        }

        List<AiGoodsCardVO> globalCards = queryCards(intent, null, null, keyword, limit, maxPrice, page);
        long globalTotal = aiGoodsQueryService.countAvailableGoodsByKeyword(null, null, keyword, maxPrice);
        return new QuerySnapshot(globalCards, globalTotal, true);
    }

    private List<AiGoodsCardVO> queryCards(
            QueryIntent intent,
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    ) {
        return switch (intent) {
            case CHEAPEST -> aiGoodsQueryService.findCheapestGoodsByKeyword(schoolCode, campusCode, keyword, limit, maxPrice, page);
            case RECOMMEND -> aiGoodsQueryService.recommendAvailableGoods(schoolCode, campusCode, keyword, limit, maxPrice, page);
            case SEARCH, GENERAL -> aiGoodsQueryService.searchAvailableGoodsByKeyword(schoolCode, campusCode, keyword, limit, maxPrice, page);
        };
    }
}

