package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员范围绑定新增/更新参数
 */
@Data
public class AdminScopeBindingUpsertDTO {

    @NotNull(message = "目标用户不能为空")
    private Long userId;

    /**
     * ALL / SCHOOL / CAMPUS
     */
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    private String schoolCode;

    private String campusCode;

    private String reason;
}

