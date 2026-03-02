package com.unimarket.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    @Schema(description = "密码")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "验证码UUID")
    @NotBlank(message = "验证码UUID不能为空")
    private String uuid;

    @Schema(description = "验证码")
    @NotBlank(message = "验证码不能为空")
    private String code;
}
