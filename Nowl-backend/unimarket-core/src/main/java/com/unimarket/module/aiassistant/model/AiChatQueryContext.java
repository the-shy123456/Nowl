package com.unimarket.module.aiassistant.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * AI聊天商品查询上下文
 */
@Data
public class AiChatQueryContext {

    /**
     * 查询意图（recommend/search/cheapest）
     */
    @Size(max = 32, message = "查询意图长度不能超过32")
    private String intent;

    /**
     * 查询关键词
     */
    @Size(max = 100, message = "关键词长度不能超过100")
    private String keyword;

    /**
     * 返回条数（最大10）
     */
    @Min(value = 1, message = "limit最小为1")
    @Max(value = 10, message = "limit最大为10")
    private Integer limit;

    /**
     * 价格上限
     */
    @DecimalMin(value = "0", inclusive = true, message = "价格上限不能为负数")
    private BigDecimal maxPrice;

    /**
     * 分页序号（从0开始）
     */
    @Min(value = 0, message = "page最小为0")
    @Max(value = 1000, message = "page最大为1000")
    private Integer page;

    /**
     * 是否请求“换一批”
     */
    private Boolean switchBatch;
}
