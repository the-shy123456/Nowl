package com.unimarket.module.aiassistant.service;

import com.unimarket.ai.vo.AiGoodsCardVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI商品查询服务
 */
public interface AiGoodsQueryService {

    /**
     * 按关键词检索在售商品
     */
    List<AiGoodsCardVO> searchAvailableGoodsByKeyword(
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    );

    /**
     * 查询关键词商品中的最低价商品
     */
    List<AiGoodsCardVO> findCheapestGoodsByKeyword(
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    );

    /**
     * 推荐在售商品
     */
    List<AiGoodsCardVO> recommendAvailableGoods(
            String schoolCode,
            String campusCode,
            String keyword,
            int limit,
            BigDecimal maxPrice,
            int page
    );

    /**
     * 统计关键词在售商品数量
     */
    long countAvailableGoodsByKeyword(String schoolCode, String campusCode, String keyword, BigDecimal maxPrice);
}

