package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.unimarket.module.aiassistant.model.AiChatQueryContext;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
final class AiAssistantTextSupport {

    private static final Pattern PRICE_LIMIT_PATTERN_A =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?\\s*(?:以下|以内|之内|及以下)");
    private static final Pattern PRICE_LIMIT_PATTERN_B =
            Pattern.compile("(?:不超过|最多|至多|低于|少于|小于等于|<=?)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?");
    private static final Pattern LEADING_QUERY_PATTERN =
            Pattern.compile("^(?:有没有人发布|有没有人卖|有没有卖|有人发布|有人卖|有没有|有无|帮我找一下|帮我搜一下|帮我找|帮我搜|找一下|搜一下|搜索一下|搜索|查询一下|查询|找找|找|搜|想买|想要|给我推荐一下|帮我推荐一下|给我推荐|帮我推荐|推荐一下|推荐下|推荐|最便宜的|最低价的|最划算的|最便宜|最低价|最划算)+");
    private static final Pattern TRAILING_TONE_PATTERN =
            Pattern.compile("(?:多少钱|什么价|价格|有吗|有没有|吗|呢|吧|呀|啊|一下|一下子|看看|看下|噢|哦|\\?|？|!|！)+$");

    private static final String[] KEYWORD_STOP_WORDS = {
            "有没有人发布", "有没有人卖", "有没有卖", "有人发布", "有人卖", "有没有", "有无",
            "最便宜", "最低价", "最划算", "多少钱", "什么价", "价格",
            "帮我", "给我", "推荐一下", "推荐下", "推荐", "找一下", "搜一下", "搜索一下", "搜索", "查询一下", "查询",
            "找", "搜", "想买", "想要", "发布", "出售", "卖",
            "一个", "一些", "请问", "麻烦", "一下", "一下子", "看看", "看下"
    };

    private AiAssistantTextSupport() {
    }

    static boolean isPricingOrAuditRequest(String message) {
        if (StrUtil.isBlank(message)) {
            return false;
        }
        return containsAny(message, "估价", "定价", "值多少钱", "值多钱", "审核", "合规", "违规", "风险等级");
    }

    static QueryIntent resolveIntent(String message, String imageUrl) {
        if (StrUtil.isNotBlank(imageUrl) || StrUtil.isBlank(message)) {
            return QueryIntent.GENERAL;
        }

        String normalized = normalizeMessage(message);
        if (containsAny(normalized,
                "退款", "纠纷", "投诉", "跑腿", "订单", "认证",
                "登录", "密码", "账户", "余额", "风控", "封禁", "申诉")) {
            return QueryIntent.GENERAL;
        }
        if (containsAny(normalized, "最便宜", "最低价", "最低多少钱", "最划算")) {
            return QueryIntent.CHEAPEST;
        }
        if (containsAny(normalized, "推荐", "给我推荐", "帮我推荐")) {
            return QueryIntent.RECOMMEND;
        }
        if (containsAny(normalized,
                "有没有", "有无", "有人发布", "有人卖",
                "找", "搜", "搜索", "查询", "想买", "多少钱")) {
            return QueryIntent.SEARCH;
        }
        return QueryIntent.GENERAL;
    }

    static boolean isSwitchBatchRequest(String message, AiChatQueryContext queryContext) {
        if (queryContext != null && Boolean.TRUE.equals(queryContext.getSwitchBatch())) {
            return true;
        }
        if (StrUtil.isBlank(message)) {
            return false;
        }
        String normalized = normalizeMessage(message);
        return containsAny(normalized, "换一批", "换一波", "换一组", "再来一批", "再推荐");
    }

    static String resolveFallbackKeyword(String message, QueryIntent intent) {
        if (StrUtil.isBlank(message) || intent == QueryIntent.GENERAL) {
            return null;
        }
        return cleanupKeyword(message);
    }

    static String cleanupKeyword(String rawKeyword) {
        if (StrUtil.isBlank(rawKeyword)) {
            return null;
        }
        if ("null".equalsIgnoreCase(rawKeyword.trim())) {
            return null;
        }

        String cleaned = rawKeyword;
        cleaned = LEADING_QUERY_PATTERN.matcher(cleaned).replaceFirst(" ");
        for (String stopWord : KEYWORD_STOP_WORDS) {
            cleaned = StrUtil.replace(cleaned, stopWord, " ");
        }
        cleaned = cleaned.replaceAll("\\d+(?:\\.\\d+)?\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?\\s*(?:以下|以内|之内|及以下)", " ");
        cleaned = cleaned.replaceAll("(?:不超过|最多|至多|低于|少于|小于等于|<=?)\\s*\\d+(?:\\.\\d+)?\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?", " ");
        cleaned = cleaned.replaceAll("(?:一批|一波|一组|一堆)", " ");
        cleaned = cleaned.replaceAll("[\\p{Punct}，。！？、；：]+", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[的了这款这个这本]+", "");
        cleaned = TRAILING_TONE_PATTERN.matcher(cleaned).replaceFirst("").trim();

        if (cleaned.length() > 24) {
            cleaned = cleaned.substring(0, 24).trim();
        }
        if ("商品".equals(cleaned) || "二手".equals(cleaned) || "东西".equals(cleaned) || "好物".equals(cleaned)) {
            return null;
        }
        return StrUtil.isBlank(cleaned) ? null : cleaned;
    }

    static BigDecimal extractMaxPrice(String message) {
        if (StrUtil.isBlank(message)) {
            return null;
        }
        Matcher matcher = PRICE_LIMIT_PATTERN_A.matcher(message);
        if (matcher.find() && matcher.groupCount() > 0) {
            return parsePositivePrice(matcher.group(1));
        }
        matcher = PRICE_LIMIT_PATTERN_B.matcher(message);
        if (matcher.find() && matcher.groupCount() > 0) {
            return parsePositivePrice(matcher.group(1));
        }
        return null;
    }

    private static BigDecimal parsePositivePrice(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (Exception ex) {
            log.debug("解析价格参数失败，忽略该价格提示: {}", ex.getMessage());
            return null;
        }
    }

    static String normalizeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.replace('？', '?')
                .replace('！', '!')
                .replace('，', ',')
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);
    }

    static boolean containsAny(String source, String... candidates) {
        for (String candidate : candidates) {
            if (source.contains(candidate)) {
                return true;
            }
        }
        return false;
    }
}
