package com.unimarket.module.aiassistant.service.impl;

import cn.hutool.core.util.StrUtil;
import com.unimarket.ai.dto.GoodsPriceEstimateDTO;
import com.unimarket.ai.service.AiPricingService;
import com.unimarket.ai.vo.GoodsPriceEstimateVO;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.aiassistant.service.AiGoodsCapabilityService;
import com.unimarket.module.goods.service.GoodsReferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 商品能力编排服务实现
 */
@Service
@RequiredArgsConstructor
public class AiGoodsCapabilityServiceImpl implements AiGoodsCapabilityService {

    private static final int PRICE_REFERENCE_LIMIT = 20;

    private final AiPricingService aiPricingService;
    private final GoodsReferenceService goodsReferenceService;

    @Override
    public GoodsPriceEstimateVO estimatePriceForPublish(GoodsPriceEstimateDTO dto) {
        return estimatePrice(dto, true);
    }

    @Override
    public GoodsPriceEstimateVO estimatePriceForAssistant(GoodsPriceEstimateDTO dto) {
        return estimatePrice(dto, false);
    }

    private GoodsPriceEstimateVO estimatePrice(GoodsPriceEstimateDTO dto, boolean requireCategoryId) {
        if (dto == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请求参数不能为空");
        }
        if (StrUtil.isBlank(dto.getTitle())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "商品标题不能为空");
        }

        String referenceData = "";
        if (dto.getCategoryId() != null) {
            referenceData = goodsReferenceService.buildPriceReferenceData(dto.getCategoryId(), PRICE_REFERENCE_LIMIT);
        } else if (requireCategoryId) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请选择商品分类");
        }

        dto.setReferenceData(referenceData);
        GoodsPriceEstimateVO result = aiPricingService.estimatePrice(dto);
        if (dto.getCategoryId() != null) {
            result.setReferenceCount(goodsReferenceService.countPriceReferenceGoods(dto.getCategoryId()));
        } else {
            result.setReferenceCount(0);
        }
        return result;
    }
}

