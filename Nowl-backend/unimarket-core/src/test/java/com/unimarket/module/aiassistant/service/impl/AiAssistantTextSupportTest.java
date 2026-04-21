package com.unimarket.module.aiassistant.service.impl;

import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AiAssistantTextSupportTest {

    @Test
    @DisplayName("cleanupKeyword: 去掉停用词与语气词，保留有效关键词")
    void cleanupKeyword_stripStopWords() {
        assertEquals("iPad", AiAssistantTextSupport.cleanupKeyword("帮我找一下 iPad 吗"));
        assertNull(AiAssistantTextSupport.cleanupKeyword("商品"));
        assertNull(AiAssistantTextSupport.cleanupKeyword("null"));
    }

    @Test
    @DisplayName("resolveIntent: 基于规则识别查询意图")
    void resolveIntent_ruleBased() {
        assertEquals(QueryIntent.CHEAPEST, AiAssistantTextSupport.resolveIntent("最便宜的耳机多少钱", null));
        assertEquals(QueryIntent.RECOMMEND, AiAssistantTextSupport.resolveIntent("推荐一下键盘", null));
        assertEquals(QueryIntent.SEARCH, AiAssistantTextSupport.resolveIntent("有没有人卖相机", null));
        assertEquals(QueryIntent.GENERAL, AiAssistantTextSupport.resolveIntent("我要退款怎么操作", null));
        assertEquals(QueryIntent.GENERAL, AiAssistantTextSupport.resolveIntent("随便聊聊", "http://img"));
    }

    @Test
    @DisplayName("resolveFallbackKeyword: 回退模式下直接清洗用户原话")
    void resolveFallbackKeyword_cleanMessage() {
        assertEquals("键盘", AiAssistantTextSupport.resolveFallbackKeyword("推荐200块钱以内的键盘一下", QueryIntent.RECOMMEND));
        assertEquals("相机", AiAssistantTextSupport.resolveFallbackKeyword("有没有人卖相机", QueryIntent.SEARCH));
        assertEquals("耳机", AiAssistantTextSupport.resolveFallbackKeyword("最便宜的耳机多少钱", QueryIntent.CHEAPEST));
        assertNull(AiAssistantTextSupport.resolveFallbackKeyword("随便聊聊", QueryIntent.GENERAL));
    }

    @Test
    @DisplayName("extractMaxPrice: 支持“以内/不超过”价格上限解析")
    void extractMaxPrice_parse() {
        assertEquals(new BigDecimal("200"), AiAssistantTextSupport.extractMaxPrice("耳机200元以内"));
        assertEquals(new BigDecimal("99.5"), AiAssistantTextSupport.extractMaxPrice("预算不超过 99.5"));
        assertNull(AiAssistantTextSupport.extractMaxPrice("不限制预算"));
    }

    @Test
    @DisplayName("isSwitchBatchRequest: 支持上下文标记与自然语言触发")
    void isSwitchBatchRequest_detect() {
        assertEquals(true, AiAssistantTextSupport.isSwitchBatchRequest("换一批", null));

        AiChatQueryContext ctx = new AiChatQueryContext();
        ctx.setSwitchBatch(true);
        assertEquals(true, AiAssistantTextSupport.isSwitchBatchRequest("", ctx));
    }
}
