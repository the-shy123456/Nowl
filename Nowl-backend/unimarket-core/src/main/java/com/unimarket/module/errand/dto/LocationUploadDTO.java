package com.unimarket.module.errand.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "位置上报DTO")
public class LocationUploadDTO {

    @Schema(description = "任务ID")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "纬度")
    @NotNull(message = "纬度不能为空")
    private BigDecimal latitude;

    @Schema(description = "经度")
    @NotNull(message = "经度不能为空")
    private BigDecimal longitude;
}
