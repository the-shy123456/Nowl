package com.unimarket.module.aiassistant.service.impl;

import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiAssistantQuerySupportTest {

    @Test
    @DisplayName("resolveContextConstraints: 非换一批请求时 page 强制归零")
    void resolveContextConstraints_resetPageWhenNotSwitchBatch() {
        AiChatQueryContext ctx = new AiChatQueryContext();
        ctx.setKeyword("键盘");
        ctx.setLimit(5);
        ctx.setPage(7);
        ctx.setMaxPrice(new BigDecimal("100"));

        QueryConstraints constraints = AiAssistantQuerySupport.resolveContextConstraints(
                ctx,
                QueryIntent.SEARCH,
                false
        );

        assertEquals("键盘", constraints.keyword);
        assertEquals(5, constraints.limit);
        assertEquals(0, constraints.page);
        assertEquals(new BigDecimal("100"), constraints.maxPrice);
    }

    @Test
    @DisplayName("resolveContextConstraints: cheapest 强制 limit=1")
    void resolveContextConstraints_cheapestForceSingle() {
        AiChatQueryContext ctx = new AiChatQueryContext();
        ctx.setKeyword("耳机");
        ctx.setLimit(10);
        ctx.setPage(3);

        QueryConstraints constraints = AiAssistantQuerySupport.resolveContextConstraints(
                ctx,
                QueryIntent.CHEAPEST,
                true
        );

        assertEquals("耳机", constraints.keyword);
        assertEquals(1, constraints.limit);
        assertEquals(3, constraints.page);
        assertNull(constraints.maxPrice);
    }

    @Test
    @DisplayName("resolveConstraintsByIntent: cheapest 强制 limit=1 且 page=0")
    void resolveConstraintsByIntent_cheapestForcePageZero() {
        QueryConstraints base = new QueryConstraints("耳机", 10, new BigDecimal("200"), 5);

        QueryConstraints resolved = AiAssistantQuerySupport.resolveConstraintsByIntent(
                QueryIntent.CHEAPEST,
                base,
                base.keyword
        );

        assertEquals("耳机", resolved.keyword);
        assertEquals(1, resolved.limit);
        assertEquals(0, resolved.page);
        assertEquals(new BigDecimal("200"), resolved.maxPrice);
    }

    @Test
    @DisplayName("resolveResponseConstraints: 优先使用模型返回参数，缺失时回退到上下文")
    void resolveResponseConstraints_preferModelValues() {
        AiChatResponseVO response = new AiChatResponseVO();
        response.setKeyword("机械键盘");
        response.setQueryLimit(6);
        response.setQueryPage(2);
        response.setMaxPrice(new BigDecimal("200"));

        QueryConstraints fallback = new QueryConstraints("键盘", 3, new BigDecimal("300"), 1);

        QueryConstraints resolved = AiAssistantQuerySupport.resolveResponseConstraints(
                QueryIntent.RECOMMEND,
                response,
                fallback
        );

        assertEquals("机械键盘", resolved.keyword);
        assertEquals(6, resolved.limit);
        assertEquals(2, resolved.page);
        assertEquals(new BigDecimal("200"), resolved.maxPrice);
    }

    @Test
    @DisplayName("resolveFallbackConstraints: 兜底链路只清洗关键词并保留预算")
    void resolveFallbackConstraints_cleanKeywordAndBudget() {
        QueryConstraints resolved = AiAssistantQuerySupport.resolveFallbackConstraints(
                "推荐200块钱以内的键盘一下",
                QueryIntent.RECOMMEND,
                null,
                false
        );

        assertEquals("键盘", resolved.keyword);
        assertEquals(AiAssistantQuerySupport.RECOMMEND_CARD_LIMIT, resolved.limit);
        assertEquals(0, resolved.page);
        assertEquals(new BigDecimal("200"), resolved.maxPrice);
    }
}
