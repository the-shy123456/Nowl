package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unimarket.ai.service.AiChatService;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.ai.vo.AiGoodsCardVO;
import com.unimarket.module.aiassistant.service.AiChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * AI 函数调用编排：
 * - 让模型输出结构化 JSON（intent/keyword/replyText）
 * - cards 一律由服务端实时查询填充，严禁信任模型编造内容
 */
@Slf4j
@RequiredArgsConstructor
final class AiAssistantFunctionCallingEngine {

    private static final int HISTORY_CONTEXT_LIMIT = 10;

    private static final String GOODS_FUNCTION_CALL_PROMPT = """
            你是 UniMarket 的智能助手 Nowl AI。
            你可以调用工具获取真实商品数据：
            - search_goods：按关键词检索在售商品
            - cheapest_goods：查询关键词最低价商品
            - recommend_goods：按关键词推荐商品

            【核心规则——严禁编造】
            1. 当用户询问商品相关问题（搜索、推荐、最低价等）时，你 **必须先调用对应的工具** 获取数据。
            2. **严禁凭空编造任何商品的名称、价格、卖家等信息。** 所有商品相关描述必须基于工具返回的实际数据。
            3. 如果工具返回 0 条结果，你必须如实告知用户“暂未找到相关商品”，绝不可自行捏造。
            4. 非商品类问题可直接回答，不调用工具。

            【输出格式】
            最终输出必须是严格 JSON（不要 Markdown、不要代码块、不要额外解释）：
            {
              "replyText": "基于工具返回数据撰写的回复文案",
              "intent": "general|search|cheapest|recommend",
              "keyword": "关键词或null"
            }

            【注意事项】
            - **不要在 JSON 中包含 cards 字段**，商品卡片由系统自动填充，你只需要输出上面三个字段。
            - replyText 中可以概括工具返回的商品数量、价格范围等信息，但不要逐条列举商品详情。
            - general 场景 keyword 返回 null。
            - 对 search/cheapest/recommend 场景，不允许输出"正在查询/正在查找/稍后返回"这类占位文案，必须直接给最终结果。
            """;

    private final AiChatService aiChatService;
    private final AiChatHistoryService aiChatHistoryService;
    private final AiAssistantGoodsQueryEngine goodsQueryEngine;

    AiChatResponseVO callFunctionCallingChat(
            Long userId,
            String schoolCode,
            String campusCode,
            String message,
            QueryIntent requestIntent,
            QueryConstraints constraints
    ) {
        String historyContext = aiChatHistoryService.getRecentContext(userId, HISTORY_CONTEXT_LIMIT);
        List<FunctionCallback> callbacks = buildGoodsFunctionCallbacks(schoolCode, campusCode);
        String rawResponse = aiChatService.chatWithFunctions(
                StrUtil.blankToDefault(message, "你好"),
                historyContext,
                GOODS_FUNCTION_CALL_PROMPT,
                callbacks
        );

        AiChatResponseVO response = parseFunctionCallingResponse(rawResponse);
        if (response == null) {
            return null;
        }

        QueryIntent responseIntent = QueryIntent.parseCode(response.getIntent());
        if (responseIntent != null && responseIntent != QueryIntent.GENERAL) {
            String keyword = AiAssistantTextSupport.cleanupKeyword(StrUtil.blankToDefault(response.getKeyword(), constraints.keyword));
            int limit = AiAssistantQuerySupport.resolveToolLimit(constraints.limit, AiAssistantQuerySupport.getDefaultLimit(responseIntent));
            int page = AiAssistantQuerySupport.resolveQueryPage(constraints.page);
            BigDecimal maxPrice = AiAssistantQuerySupport.normalizeMaxPrice(constraints.maxPrice);
            QuerySnapshot snapshot = goodsQueryEngine.queryWithFallback(
                    responseIntent, schoolCode, campusCode, keyword, limit, maxPrice, page);
            QueryConstraints resolved = new QueryConstraints(keyword, limit, maxPrice, page);
            response.setCards(snapshot.cards);
            response.setKeyword(keyword);
            AiAssistantQuerySupport.applyQueryMetadata(response, snapshot, resolved);
        }

        return goodsQueryEngine.ensureResolvedQueryResponse(response, requestIntent, schoolCode, campusCode, constraints);
    }

    private List<FunctionCallback> buildGoodsFunctionCallbacks(String schoolCode, String campusCode) {
        FunctionCallback searchCallback = FunctionCallbackWrapper
                .<GoodsToolInput, GoodsToolOutput>builder(input -> executeToolQuery(
                        QueryIntent.SEARCH,
                        schoolCode,
                        campusCode,
                        input,
                        AiAssistantQuerySupport.SEARCH_CARD_LIMIT
                ))
                .withName("search_goods")
                .withDescription("按关键词检索在售商品，支持参数：keyword、limit(最多10)、maxPrice(价格上限)、page(从0开始)。")
                .withInputType(GoodsToolInput.class)
                .build();

        FunctionCallback cheapestCallback = FunctionCallbackWrapper
                .<GoodsToolInput, GoodsToolOutput>builder(input -> executeToolQuery(
                        QueryIntent.CHEAPEST,
                        schoolCode,
                        campusCode,
                        input,
                        AiAssistantQuerySupport.CHEAPEST_CARD_LIMIT
                ))
                .withName("cheapest_goods")
                .withDescription("查询关键词商品中的最低价在售商品，支持参数：keyword、maxPrice。")
                .withInputType(GoodsToolInput.class)
                .build();

        FunctionCallback recommendCallback = FunctionCallbackWrapper
                .<GoodsToolInput, GoodsToolOutput>builder(input -> executeToolQuery(
                        QueryIntent.RECOMMEND,
                        schoolCode,
                        campusCode,
                        input,
                        AiAssistantQuerySupport.RECOMMEND_CARD_LIMIT
                ))
                .withName("recommend_goods")
                .withDescription("按关键词推荐在售商品，支持参数：keyword、limit(最多10)、maxPrice(价格上限)、page(从0开始，便于换一批)。")
                .withInputType(GoodsToolInput.class)
                .build();

        return List.of(searchCallback, cheapestCallback, recommendCallback);
    }

    private GoodsToolOutput executeToolQuery(
            QueryIntent intent,
            String schoolCode,
            String campusCode,
            GoodsToolInput input,
            int defaultLimit
    ) {
        String keyword = AiAssistantTextSupport.cleanupKeyword(input == null ? null : input.getKeyword());
        int limit = AiAssistantQuerySupport.resolveToolLimit(input == null ? null : input.getLimit(), defaultLimit);
        int page = AiAssistantQuerySupport.resolveQueryPage(input == null ? null : input.getPage());
        BigDecimal maxPrice = AiAssistantQuerySupport.normalizeMaxPrice(input == null ? null : input.getMaxPrice());

        QuerySnapshot snapshot = goodsQueryEngine.queryWithFallback(intent, schoolCode, campusCode, keyword, limit, maxPrice, page);

        GoodsToolOutput output = new GoodsToolOutput();
        output.setIntent(intent.toCode());
        output.setKeyword(keyword);
        output.setScopeText(AiAssistantQuerySupport.buildScopeText(snapshot.fallbackUsed, schoolCode, campusCode));
        output.setFallbackUsed(snapshot.fallbackUsed);
        output.setTotal(snapshot.total);
        output.setCards(snapshot.cards);
        output.setLimit(limit);
        output.setPage(page);
        output.setMaxPrice(maxPrice);
        output.setHasMore((long) (page + 1) * limit < snapshot.total);
        return output;
    }

    private AiChatResponseVO parseFunctionCallingResponse(String rawResponse) {
        if (StrUtil.isBlank(rawResponse)) {
            return null;
        }

        String json = extractJsonObject(rawResponse);
        if (StrUtil.isBlank(json)) {
            return null;
        }

        try {
            JSONObject data = JSONUtil.parseObj(json);

            AiChatResponseVO response = new AiChatResponseVO();
            response.setReplyText(StrUtil.blankToDefault(data.getStr("replyText"), ""));
            response.setCards(Collections.emptyList());
            response.setIntent(normalizeIntentCode(data.getStr("intent")));
            response.setKeyword(AiAssistantTextSupport.cleanupKeyword(data.getStr("keyword")));
            return response;
        } catch (Exception ex) {
            log.debug("解析函数调用返回失败，降级到规则推断: {}", ex.getMessage());
            return null;
        }
    }

    private String extractJsonObject(String rawResponse) {
        String trimmed = StrUtil.trim(rawResponse);
        if (StrUtil.isBlank(trimmed)) {
            return null;
        }

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return trimmed.substring(start, end + 1);
    }

    private String normalizeIntentCode(String rawIntent) {
        QueryIntent parsed = QueryIntent.parseCode(rawIntent);
        return parsed == null ? QueryIntent.GENERAL.toCode() : parsed.toCode();
    }
}

