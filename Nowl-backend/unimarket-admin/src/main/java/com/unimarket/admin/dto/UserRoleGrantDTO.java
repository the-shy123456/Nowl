package com.unimarket.admin.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 用户角色授予参数
 */
@Data
public class UserRoleGrantDTO {

    @NotNull(message = "目标用户不能为空")
    private Long userId;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /**
     * 为空表示长期有效
     */
    private LocalDateTime expiredTime;

    private String reason;
}

