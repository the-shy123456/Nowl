package com.unimarket.module.aiassistant.service.impl;

import com.unimarket.ai.vo.AiGoodsCardVO;

import java.util.Collections;
import java.util.List;

final class QuerySnapshot {
    final List<AiGoodsCardVO> cards;
    final long total;
    final boolean fallbackUsed;

    QuerySnapshot(List<AiGoodsCardVO> cards, long total, boolean fallbackUsed) {
        this.cards = cards == null ? Collections.emptyList() : cards;
        this.total = total;
        this.fallbackUsed = fallbackUsed;
    }
}

