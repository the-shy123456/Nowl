package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 风控模式更新请求。
 */
@Data
public class RiskModeUpdateDTO {

    @NotBlank(message = "风控模式不能为空")
    private String mode;
}
