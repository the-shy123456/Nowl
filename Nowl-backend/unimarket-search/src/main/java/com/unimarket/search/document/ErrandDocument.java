package com.unimarket.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 跑腿任务ES文档实体
 */
@Data
@Document(indexName = "errand")
@Setting(settingPath = "/elasticsearch/errand-settings.json")
@Mapping(mappingPath = "/elasticsearch/errand-mapping.json")
public class ErrandDocument {

    @Id
    private Long taskId;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart"),
        otherFields = {
            @InnerField(suffix = "pinyin", type = FieldType.Text, analyzer = "pinyin_analyzer"),
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String taskContent;

    @Field(type = FieldType.Double)
    private BigDecimal reward;

    @Field(type = FieldType.Integer)
    private Integer taskStatus;

    @Field(type = FieldType.Long)
    private Long publisherId;

    @Field(type = FieldType.Keyword)
    private String publisherName;

    @Field(type = FieldType.Keyword, index = false)
    private String publisherAvatar;

    @Field(type = FieldType.Keyword)
    private String schoolCode;

    @Field(type = FieldType.Keyword)
    private String campusCode;

    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String pickupAddress;

    @Field(type = FieldType.Text, analyzer = "ik_smart")
    private String deliveryAddress;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss||epoch_millis")
    private LocalDateTime createTime;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss||epoch_millis")
    private LocalDateTime updateTime;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS||yyyy-MM-dd'T'HH:mm:ss||epoch_millis")
    private LocalDateTime deadline;

    @Field(type = FieldType.Keyword, index = false)
    private String image;
}
