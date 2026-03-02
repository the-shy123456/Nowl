package com.unimarket.module.user.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证接口返回体（仅暴露会话相关非敏感信息）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthSessionVO {

    /**
     * 当前用户信息
     */
    private UserInfoVO userInfo;
}

