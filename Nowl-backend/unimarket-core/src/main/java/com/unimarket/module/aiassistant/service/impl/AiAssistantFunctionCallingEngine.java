package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unimarket.ai.service.AiChatService;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import com.unimarket.module.aiassistant.service.AiChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AI 函数调用编排：
 * - 由模型自主判断是否调用工具，并自行提取工具入参
 * - 最终让模型输出结构化 JSON（intent/keyword/replyText/limit/maxPrice/page）
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

            【上下文补充】
            %s

            【核心规则——严禁编造】
            1. 你要自己判断当前是否需要调用工具，并自己从用户原话里提取 keyword、limit、maxPrice、page。
            2. 当用户询问商品相关问题（搜索、推荐、最低价等）时，你 **必须先调用对应的工具** 获取数据。
            3. 像“推荐200块钱以内的键盘一下”这种表达里，keyword 应该是“键盘”，“一下/吧/吗”只是语气词，不能当关键词。
            4. 如果上下文里的 switchBatch=true，表示前端已经把“换一批”所需的下一页 page 和上一次查询条件带过来了；除非用户明确提出了新条件，否则优先沿用上下文里的 keyword、limit、maxPrice、page。
            5. **严禁凭空编造任何商品的名称、价格、卖家等信息。** 所有商品相关描述必须基于工具返回的实际数据。
            6. 如果工具返回 0 条结果，你必须如实告知用户“暂未找到相关商品”，绝不可自行捏造。
            7. 非商品类问题可直接回答，不调用工具。

            【输出格式】
            最终输出必须是严格 JSON（不要 Markdown、不要代码块、不要额外解释）：
            {
              "replyText": "基于工具返回数据撰写的回复文案",
              "intent": "general|search|cheapest|recommend",
              "keyword": "关键词或null",
              "limit": 1-10 或 null,
              "maxPrice": 数字或 null,
              "page": 大于等于0的整数或 null
            }

            【注意事项】
            - **不要在 JSON 中包含 cards 字段**，商品卡片由系统自动填充，你只需要输出上面六个字段。
            - replyText 中可以概括工具返回的商品数量、价格范围等信息，但不要逐条列举商品详情。
            - general 场景 keyword、limit、maxPrice、page 都返回 null。
            - 对 search/cheapest/recommend 场景，intent/keyword/limit/maxPrice/page 需要与最终使用的查询条件保持一致。
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
            AiChatQueryContext queryContext,
            boolean switchBatchRequest
    ) {
        String historyContext = aiChatHistoryService.getRecentContext(userId, HISTORY_CONTEXT_LIMIT);
        List<FunctionCallback> callbacks = buildGoodsFunctionCallbacks(schoolCode, campusCode);
        QueryIntent contextIntent = AiAssistantQuerySupport.resolveContextIntent(queryContext);
        QueryConstraints contextConstraints = AiAssistantQuerySupport.resolveContextConstraints(
                queryContext,
                contextIntent,
                switchBatchRequest
        );
        String rawResponse = aiChatService.chatWithFunctions(
                StrUtil.blankToDefault(message, "你好"),
                historyContext,
                buildFunctionCallingPrompt(queryContext, switchBatchRequest),
                callbacks
        );

        AiChatResponseVO response = parseFunctionCallingResponse(rawResponse);
        if (response == null) {
            return null;
        }

        QueryIntent responseIntent = QueryIntent.parseCode(response.getIntent());
        QueryIntent effectiveIntent = responseIntent == null
                ? (contextIntent == null ? QueryIntent.GENERAL : contextIntent)
                : responseIntent;
        response.setIntent(effectiveIntent.toCode());

        if (effectiveIntent != QueryIntent.GENERAL) {
            QueryConstraints resolved = AiAssistantQuerySupport.resolveResponseConstraints(
                    effectiveIntent,
                    response,
                    contextConstraints
            );
            QuerySnapshot snapshot = goodsQueryEngine.queryWithFallback(
                    effectiveIntent,
                    schoolCode,
                    campusCode,
                    resolved.keyword,
                    resolved.limit,
                    resolved.maxPrice,
                    resolved.page
            );
            response.setCards(snapshot.cards);
            response.setKeyword(resolved.keyword);
            AiAssistantQuerySupport.applyQueryMetadata(response, snapshot, resolved);
        }

        return goodsQueryEngine.ensureResolvedQueryResponse(
                response,
                contextIntent,
                schoolCode,
                campusCode,
                contextConstraints
        );
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
            response.setQueryLimit(normalizeNullableLimit(data.getInt("limit")));
            response.setQueryPage(normalizeNullablePage(data.getInt("page")));
            response.setMaxPrice(parseNullableMaxPrice(data.get("maxPrice")));
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

    private String buildFunctionCallingPrompt(AiChatQueryContext queryContext, boolean switchBatchRequest) {
        return GOODS_FUNCTION_CALL_PROMPT.formatted(buildRuntimeContextPrompt(queryContext, switchBatchRequest));
    }

    private String buildRuntimeContextPrompt(AiChatQueryContext queryContext, boolean switchBatchRequest) {
        if (queryContext == null) {
            return switchBatchRequest
                    ? "switchBatch=true，但当前没有可复用的上一轮查询上下文；如果历史消息也无法明确上一轮条件，请直接让用户补充关键词。"
                    : "当前没有额外的查询上下文。";
        }

        return """
                - previousIntent: %s
                - previousKeyword: %s
                - previousLimit: %s
                - previousMaxPrice: %s
                - previousPage: %s
                - switchBatch: %s
                """.formatted(
                safeContextValue(queryContext.getIntent()),
                safeContextValue(queryContext.getKeyword()),
                safeContextValue(queryContext.getLimit()),
                safeContextValue(queryContext.getMaxPrice() == null ? null : queryContext.getMaxPrice().stripTrailingZeros().toPlainString()),
                safeContextValue(queryContext.getPage()),
                switchBatchRequest
        ).trim();
    }

    private String safeContextValue(Object value) {
        if (value == null) {
            return "null";
        }
        String text = Objects.toString(value, "").trim();
        return text.isEmpty() ? "null" : text;
    }

    private Integer normalizeNullableLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return null;
        }
        return Math.min(limit, AiAssistantQuerySupport.TOOL_CARD_MAX_LIMIT);
    }

    private Integer normalizeNullablePage(Integer page) {
        if (page == null || page < 0) {
            return null;
        }
        return page;
    }

    private BigDecimal parseNullableMaxPrice(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        String text = Objects.toString(rawValue, "").trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }

        try {
            BigDecimal value = new BigDecimal(text);
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (Exception ex) {
            log.debug("解析模型返回的 maxPrice 失败，忽略该字段: {}", ex.getMessage());
            return null;
        }
    }
}
