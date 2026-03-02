package com.unimarket.module.aiassistant.service.impl;

import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiAssistantQuerySupportTest {

    @Test
    @DisplayName("resolveQueryConstraints: 非换一批请求时 page 强制归零")
    void resolveQueryConstraints_resetPageWhenNotSwitchBatch() {
        AiChatQueryContext ctx = new AiChatQueryContext();
        ctx.setKeyword("键盘");
        ctx.setLimit(5);
        ctx.setPage(7);
        ctx.setMaxPrice(new BigDecimal("100"));

        QueryConstraints constraints = AiAssistantQuerySupport.resolveQueryConstraints(
                "",
                QueryIntent.SEARCH,
                ctx,
                false
        );

        assertEquals("键盘", constraints.keyword);
        assertEquals(5, constraints.limit);
        assertEquals(0, constraints.page);
        assertEquals(new BigDecimal("100"), constraints.maxPrice);
    }

    @Test
    @DisplayName("resolveQueryConstraints: cheapest 强制 limit=1")
    void resolveQueryConstraints_cheapestForceSingle() {
        AiChatQueryContext ctx = new AiChatQueryContext();
        ctx.setKeyword("耳机");
        ctx.setLimit(10);
        ctx.setPage(3);

        QueryConstraints constraints = AiAssistantQuerySupport.resolveQueryConstraints(
                "最便宜的耳机",
                QueryIntent.CHEAPEST,
                ctx,
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
}
