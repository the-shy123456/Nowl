package com.unimarket.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商品估价请求DTO
 */
@Data
public class GoodsPriceEstimateDTO {

    /**
     * 商品标题
     */
    @NotBlank(message = "商品标题不能为空")
    @Size(max = 120, message = "商品标题长度不能超过120")
    private String title;

    /**
     * 商品描述
     */
    @Size(max = 2000, message = "商品描述长度不能超过2000")
    private String description;

    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    /**
     * 商品图片URL
     */
    @Size(max = 1024, message = "图片地址长度不能超过1024")
    private String imageUrl;

    /**
     * 商品成色（1-10）
     */
    @NotNull(message = "商品成色不能为空")
    @Min(value = 1, message = "商品成色最小为1")
    @Max(value = 10, message = "商品成色最大为10")
    private Integer itemCondition;

    /**
     * 同类商品参考数据（由调用方提供）
     */
    @Size(max = 4000, message = "参考数据长度不能超过4000")
    private String referenceData;
}
