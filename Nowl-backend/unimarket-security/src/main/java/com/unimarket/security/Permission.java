package com.unimarket.security;

/**
 * 权限枚举类，用于定义不同的权限类型
 */
public enum Permission {
    /**
     * 未登录用户
     */
    ANONYMOUS,
    /**
     * 已登录用户
     */
    LOGGED_IN,
    /**
     * 已认证用户（审核通过）
     */
    AUTHENTICATED,
    /**
     * 管理员
     */
    ADMIN
}