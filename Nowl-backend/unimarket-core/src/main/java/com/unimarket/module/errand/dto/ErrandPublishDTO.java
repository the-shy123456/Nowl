package com.unimarket.module.errand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 跑腿任务发布DTO
 */
@Data
public class ErrandPublishDTO {
    
    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    private String title;
    
    /**
     * 任务描述
     */
    private String description;
    
    /**
     * 任务内容
     */
    @NotBlank(message = "任务内容不能为空")
    private String taskContent;

    /**
     * 任务图片（JSON格式）
     */
    private String imageList;

    /**
     * 取件地址
     */
    @Schema(description = "取件地址")
    @NotBlank(message = "取件地址不能为空")
    private String pickupAddress;

    @Schema(description = "取件纬度")
    private BigDecimal pickupLatitude;

    @Schema(description = "取件经度")
    private BigDecimal pickupLongitude;
    
    /**
     * 送达地址
     */
    @Schema(description = "送达地址")
    @NotBlank(message = "送达地址不能为空")
    private String deliveryAddress;

    @Schema(description = "送达纬度")
    private BigDecimal deliveryLatitude;

    @Schema(description = "送达经度")
    private BigDecimal deliveryLongitude;
    
    /**
     * 报酬金额
     */
    @NotNull(message = "报酬金额不能为空")
    private BigDecimal reward;
    
    /**
     * 截止时间
     */
    private String deadline;
    
    /**
     * 备注
     */
    private String remark;

    /**
     * 学校编码
     */
    @NotBlank(message = "学校编码不能为空")
    private String schoolCode;

    /**
     * 校区编码
     */
    @NotBlank(message = "校区编码不能为空")
    private String campusCode;
}
