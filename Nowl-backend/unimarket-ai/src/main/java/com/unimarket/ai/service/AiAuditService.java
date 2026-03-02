package com.unimarket.ai.service;

import com.unimarket.ai.dto.AiAuditResult;

/**
 * AI审核Service接口
 */
public interface AiAuditService {

    /**
     * 审核文本内容
     * @param content 待审核文本
     * @return 审核结果
     */
    AiAuditResult auditText(String content);

    /**
     * 审核图片内容
     * @param imageUrl 图片URL
     * @return 审核结果（含风险等级）
     */
    AiAuditResult auditImage(String imageUrl);

    /**
     * 获取AI定价建议
     * @param title 商品标题
     * @param description 商品描述
     * @param categoryName 分类名称
     * @return 建议价格
     */
    Double getPriceSuggestion(String title, String description, String categoryName);
}
