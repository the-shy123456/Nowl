package com.unimarket.ai.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI聊天结构化响应
 */
@Data
public class AiChatResponseVO {

    /**
     * 回复文本
     */
    private String replyText;

    /**
     * 商品卡片列表
     */
    private List<AiGoodsCardVO> cards;

    /**
     * 意图标签
     */
    private String intent;

    /**
     * 解析后的关键词
     */
    private String keyword;

    /**
     * 本次商品查询返回条数
     */
    private Integer queryLimit;

    /**
     * 本次商品查询页码（从0开始）
     */
    private Integer queryPage;

    /**
     * 本次商品查询价格上限
     */
    private BigDecimal maxPrice;

    /**
     * 本次查询总条数
     */
    private Long total;

    /**
     * 是否还有下一批结果
     */
    private Boolean hasMore;
}
