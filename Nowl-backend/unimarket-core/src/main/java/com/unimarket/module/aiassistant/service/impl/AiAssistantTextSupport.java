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

    private static final Pattern CHEAPEST_KEYWORD_PATTERN =
            Pattern.compile("(?:最便宜|最低价|最划算)的?(.+?)(?:多少钱|什么价|价格|有吗|有没有|吗|呢|\\?|？|$)");
    private static final Pattern HAS_GOODS_PATTERN =
            Pattern.compile("(?:有没有人发布|有人发布|有人卖|有没有|有无)(.+?)(?:吗|呢|\\?|？|$)");
    private static final Pattern SEARCH_KEYWORD_PATTERN =
            Pattern.compile("(?:帮我找|帮我搜|找一下|搜索|查询|找|搜|想买|想要)(.+?)(?:吗|呢|吧|\\?|？|$)");
    private static final Pattern RECOMMEND_KEYWORD_PATTERN =
            Pattern.compile("(?:推荐|给我推荐|帮我推荐|推荐一下)(.+?)(?:吗|呢|吧|\\?|？|$)");
    private static final Pattern QUANTITY_PATTERN =
            Pattern.compile("(\\d{1,2})\\s*(个|款|件|条|样)");
    private static final Pattern CHINESE_QUANTITY_PATTERN =
            Pattern.compile("([零一二三四五六七八九十两俩]{1,3})\\s*(个|款|件|条|样)");
    private static final Pattern PRICE_LIMIT_PATTERN_A =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?\\s*(?:以下|以内|之内|及以下)");
    private static final Pattern PRICE_LIMIT_PATTERN_B =
            Pattern.compile("(?:不超过|最多|至多|低于|少于|小于等于|<=?)\\s*(\\d+(?:\\.\\d+)?)\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?");

    private static final String[] KEYWORD_STOP_WORDS = {
            "有没有", "有无", "有人发布", "有人卖", "有没有人发布",
            "有人", "人发布",
            "最便宜", "最低价", "最划算", "多少钱", "什么价", "价格",
            "帮我", "给我", "推荐", "推荐一下", "找一下", "搜索", "查询",
            "找", "搜", "想买", "想要", "发布", "出售", "卖",
            "一个", "一些", "请问", "麻烦"
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

    static String extractKeyword(String message, QueryIntent intent) {
        if (StrUtil.isBlank(message) || intent == QueryIntent.GENERAL) {
            return null;
        }

        String candidate = message;
        if (intent == QueryIntent.CHEAPEST) {
            Matcher matcher = CHEAPEST_KEYWORD_PATTERN.matcher(message);
            if (matcher.find() && matcher.groupCount() > 0) {
                candidate = matcher.group(1);
            }
        } else if (intent == QueryIntent.RECOMMEND) {
            Matcher matcher = RECOMMEND_KEYWORD_PATTERN.matcher(message);
            if (matcher.find() && matcher.groupCount() > 0) {
                candidate = matcher.group(1);
            }
        } else if (intent == QueryIntent.SEARCH) {
            Matcher matcher = HAS_GOODS_PATTERN.matcher(message);
            if (matcher.find() && matcher.groupCount() > 0) {
                candidate = matcher.group(1);
            } else {
                matcher = SEARCH_KEYWORD_PATTERN.matcher(message);
                if (matcher.find() && matcher.groupCount() > 0) {
                    candidate = matcher.group(1);
                }
            }
        }

        return cleanupKeyword(candidate);
    }

    static String cleanupKeyword(String rawKeyword) {
        if (StrUtil.isBlank(rawKeyword)) {
            return null;
        }
        if ("null".equalsIgnoreCase(rawKeyword.trim())) {
            return null;
        }

        String cleaned = rawKeyword;
        for (String stopWord : KEYWORD_STOP_WORDS) {
            cleaned = StrUtil.replace(cleaned, stopWord, " ");
        }
        cleaned = cleaned.replaceAll("\\d+(?:\\.\\d+)?\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?\\s*(?:以下|以内|之内|及以下)", " ");
        cleaned = cleaned.replaceAll("(?:不超过|最多|至多|低于|少于|小于等于|<=?)\\s*\\d+(?:\\.\\d+)?\\s*(?:块|快|元|rmb|￥|¥)?(?:钱)?", " ");
        cleaned = cleaned.replaceAll("\\d{1,2}\\s*(个|款|件|条|样)", " ");
        cleaned = cleaned.replaceAll("[零一二三四五六七八九十两俩]{1,3}\\s*(个|款|件|条|样)", " ");
        cleaned = cleaned.replaceAll("(?:一批|一波|一组|一堆)", " ");
        cleaned = cleaned.replaceAll("[\\p{Punct}，。！？、；：]+", " ");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        cleaned = cleaned.replaceAll("^[的了这款这个这本]+", "");
        cleaned = cleaned.replaceAll("[吗呢吧呀啊]+$", "").trim();

        if (cleaned.length() > 24) {
            cleaned = cleaned.substring(0, 24).trim();
        }
        if ("商品".equals(cleaned) || "二手".equals(cleaned) || "东西".equals(cleaned) || "好物".equals(cleaned)) {
            return null;
        }
        return StrUtil.isBlank(cleaned) ? null : cleaned;
    }

    static Integer extractRequestedLimit(String message, QueryIntent intent) {
        if (StrUtil.isBlank(message) || intent == QueryIntent.GENERAL || intent == QueryIntent.CHEAPEST) {
            return null;
        }
        Matcher matcher = QUANTITY_PATTERN.matcher(message);
        if (matcher.find() && matcher.groupCount() > 0) {
            try {
                int value = Integer.parseInt(matcher.group(1));
                if (value > 0) {
                    return Math.min(value, AiAssistantQuerySupport.TOOL_CARD_MAX_LIMIT);
                }
            } catch (Exception ex) {
                log.debug("解析数量参数失败，忽略该数量提示: {}", ex.getMessage());
                return null;
            }
        }
        matcher = CHINESE_QUANTITY_PATTERN.matcher(message);
        if (matcher.find() && matcher.groupCount() > 0) {
            Integer value = parseChineseNumber(matcher.group(1));
            if (value != null && value > 0) {
                return Math.min(value, AiAssistantQuerySupport.TOOL_CARD_MAX_LIMIT);
            }
        }
        return null;
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

    private static Integer parseChineseNumber(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        String text = raw.trim()
                .replace("两", "二")
                .replace("俩", "二");
        if ("十".equals(text)) {
            return 10;
        }
        if (text.startsWith("十") && text.length() == 2) {
            int unit = chineseDigit(text.charAt(1));
            return unit >= 0 ? 10 + unit : null;
        }
        if (text.endsWith("十") && text.length() == 2) {
            int tens = chineseDigit(text.charAt(0));
            return tens > 0 ? tens * 10 : null;
        }
        if (text.length() == 3 && text.charAt(1) == '十') {
            int tens = chineseDigit(text.charAt(0));
            int unit = chineseDigit(text.charAt(2));
            if (tens > 0 && unit >= 0) {
                return tens * 10 + unit;
            }
            return null;
        }
        if (text.length() == 1) {
            int value = chineseDigit(text.charAt(0));
            return value >= 0 ? value : null;
        }
        return null;
    }

    private static int chineseDigit(char ch) {
        return switch (ch) {
            case '零' -> 0;
            case '一' -> 1;
            case '二' -> 2;
            case '三' -> 3;
            case '四' -> 4;
            case '五' -> 5;
            case '六' -> 6;
            case '七' -> 7;
            case '八' -> 8;
            case '九' -> 9;
            default -> -1;
        };
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

