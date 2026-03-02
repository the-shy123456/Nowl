package com.unimarket.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品ES文档实体
 * 用于Elasticsearch全文搜索
 */
@Data
@Document(indexName = "goods")
@Setting(settingPath = "/elasticsearch/goods-settings.json")
@Mapping(mappingPath = "/elasticsearch/goods-mapping.json")
public class GoodsDocument {

    @Id
    private Long productId;

    /**
     * 商品标题 - 支持中文分词和拼音搜索
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"),
        otherFields = {
            @InnerField(suffix = "pinyin", type = FieldType.Text, analyzer = "pinyin_analyzer"),
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String title;

    /**
     * 商品描述
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    /**
     * 分类ID
     */
    @Field(type = FieldType.Integer)
    private Integer categoryId;

    /**
     * 分类名称
     */
    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "ik_smart"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String categoryName;

    /**
     * 价格
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 原价
     */
    @Field(type = FieldType.Double)
    private BigDecimal originalPrice;

    /**
     * 卖家ID
     */
    @Field(type = FieldType.Long)
    private Long sellerId;

    /**
     * 卖家昵称
     */
    @Field(type = FieldType.Keyword)
    private String sellerName;

    /**
     * 卖家头像
     */
    @Field(type = FieldType.Keyword, index = false)
    private String sellerAvatar;

    /**
     * 卖家认证状态
     */
    @Field(type = FieldType.Integer)
    private Integer sellerAuthStatus;

    /**
     * 学校编码
     */
    @Field(type = FieldType.Keyword)
    private String schoolCode;

    /**
     * 校区编码
     */
    @Field(type = FieldType.Keyword)
    private String campusCode;

    /**
     * 交易状态: 0-在售 1-已售 2-下架
     */
    @Field(type = FieldType.Integer)
    private Integer tradeStatus;

    /**
     * 审核状态: 0-待审核 1-审核通过 2-人工审核通过 3-审核拒绝
     */
    @Field(type = FieldType.Integer)
    private Integer reviewStatus;

    /**
     * 商品成色: 1-10
     */
    @Field(type = FieldType.Integer)
    private Integer itemCondition;

    /**
     * 商品图片
     */
    @Field(type = FieldType.Keyword, index = false)
    private String image;

    /**
     * 收藏数
     */
    @Field(type = FieldType.Integer)
    private Integer collectCount;

    /**
     * 浏览数
     */
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    /**
     * 热度分
     */
    @Field(type = FieldType.Double)
    private Double hotScore;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss||epoch_millis")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss||epoch_millis")
    private LocalDateTime updateTime;
}
