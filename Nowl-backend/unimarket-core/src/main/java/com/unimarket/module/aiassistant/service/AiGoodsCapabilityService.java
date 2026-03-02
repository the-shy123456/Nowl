package com.unimarket.module.aiassistant.service;

import com.unimarket.ai.dto.GoodsPriceEstimateDTO;
import com.unimarket.ai.vo.GoodsPriceEstimateVO;

/**
 * AI 商品能力编排服务（发布估价、助手估价等）
 */
public interface AiGoodsCapabilityService {

    /**
     * 商品发布场景估价（要求分类ID，带同类商品参考数据）
     */
    GoodsPriceEstimateVO estimatePriceForPublish(GoodsPriceEstimateDTO dto);

    /**
     * 聊天助手场景估价（可选分类ID，不强依赖参考数据）
     */
    GoodsPriceEstimateVO estimatePriceForAssistant(GoodsPriceEstimateDTO dto);
}

