package com.unimarket.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 用户注册DTO
 */
@Data
public class UserRegisterDTO {

    /**
     * 手机号
     */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度为6-20位")
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Length(max = 25, message = "昵称长度不能超过25个字符")
    private String nickName;

    /**
     * 短信验证码
     */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
