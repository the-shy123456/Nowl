package com.unimarket.module.user.dto;

import lombok.Data;

import jakarta.validation.constraints.Pattern;

/**
 * 用户信息修改DTO
 */
@Data
public class UserUpdateDTO {
    
    /**
     * 昵称
     */
    private String nickName;
    
    /**
     * 头像
     */
    private String avatar;

    /**
     * 证件照
     */
    private String certImage;

    /**
     * 本人照
     */
    private String selfImage;
    
    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 邮箱
     */
    @Pattern(regexp = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", message = "邮箱格式不正确")
    private String email;

    /**
     * 真实姓名
     */
    private String userName;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 性别：1-男，2-女
     */
    private Integer gender;

    /**
     * 年级
     */
    private String grade;

    /**
     * 认证状态（前端传1表示提交审核）
     */
    private Integer authStatus;
}
