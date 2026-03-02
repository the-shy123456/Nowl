package com.unimarket.module.aiassistant.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.unimarket.ai.vo.AiChatResponseVO;
import com.unimarket.ai.vo.AiGoodsCardVO;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * AI聊天历史内容编解码器
 */
@Slf4j
public final class AiChatContentCodec {

    private static final String STRUCTURED_PREFIX = "__AI_STRUCTURED__:";

    private AiChatContentCodec() {
    }

    /**
     * 编码模型回复（带卡片时序列化为结构化文本）
     */
    public static String encodeModelContent(AiChatResponseVO response) {
        if (response == null) {
            return "";
        }
        String replyText = StrUtil.blankToDefault(response.getReplyText(), "");
        List<AiGoodsCardVO> cards = response.getCards();
        if (cards == null || cards.isEmpty()) {
            return replyText;
        }

        JSONObject payload = new JSONObject();
        payload.set("replyText", replyText);
        payload.set("cards", cards);
        return STRUCTURED_PREFIX + payload;
    }

    /**
     * 解码模型回复（历史兼容：普通文本直接回传）
     */
    public static DecodedContent decodeModelContent(String content) {
        if (StrUtil.isBlank(content) || !content.startsWith(STRUCTURED_PREFIX)) {
            return new DecodedContent(StrUtil.blankToDefault(content, ""), Collections.emptyList());
        }

        try {
            String rawPayload = content.substring(STRUCTURED_PREFIX.length());
            JSONObject payload = JSONUtil.parseObj(rawPayload);
            String replyText = StrUtil.blankToDefault(payload.getStr("replyText"), "");
            JSONArray cardsArray = payload.getJSONArray("cards");
            List<AiGoodsCardVO> cards = cardsArray == null
                    ? Collections.emptyList()
                    : cardsArray.toList(AiGoodsCardVO.class);
            return new DecodedContent(replyText, cards);
        } catch (Exception ex) {
            log.debug("解析结构化AI历史内容失败，降级为纯文本: {}", ex.getMessage());
            return new DecodedContent(content, Collections.emptyList());
        }
    }

    public static final class DecodedContent {
        private final String replyText;
        private final List<AiGoodsCardVO> cards;

        public DecodedContent(String replyText, List<AiGoodsCardVO> cards) {
            this.replyText = replyText;
            this.cards = cards == null ? Collections.emptyList() : cards;
        }

        public String getReplyText() {
            return replyText;
        }

        public List<AiGoodsCardVO> getCards() {
            return cards;
        }
    }
}

