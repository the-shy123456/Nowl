package com.unimarket.ai.service;

import com.unimarket.ai.dto.GoodsPriceEstimateDTO;
import com.unimarket.ai.vo.GoodsPriceEstimateVO;

/**
 * AI 估价能力接口
 */
public interface AiPricingService {

    /**
     * 对商品进行估价
     *
     * @param dto 商品信息（可包含参考数据）
     * @return 估价结果
     */
    GoodsPriceEstimateVO estimatePrice(GoodsPriceEstimateDTO dto);
}
