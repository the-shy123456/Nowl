package com.unimarket.recommend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品相似度实体
 * 存储物品协同过滤和内容推荐的预计算结果
 */
@Data
@TableName("goods_similarity")
public class GoodsSimilarity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 相似商品ID
     */
    private Long similarProductId;

    /**
     * 相似度分数 (0-1)
     */
    private BigDecimal similarityScore;

    /**
     * 相似度类型: 1协同过滤 2内容相似
     */
    private Integer similarityType;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 相似度类型枚举
     */
    public static class SimilarityType {
        public static final int COLLABORATIVE = 1; // 协同过滤
        public static final int CONTENT_BASED = 2; // 内容相似
    }
}
