package com.unimarket.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 返回状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /**
     * 成功
     */
    SUCCESS(200, "操作成功"),

    /**
     * 失败
     */
    ERROR(500, "操作失败"),

    /**
     * 参数错误
     */
    PARAM_ERROR(400, "参数错误"),
    
    /**
     * 参数无效
     */
    PARAM_IS_INVALID(400, "参数无效"),

    /**
     * 未授权
     */
    UNAUTHORIZED(401, "未授权，请先登录"),

    /**
     * 禁止访问
     */
    FORBIDDEN(403, "禁止访问"),

    /**
     * 资源不存在
     */
    NOT_FOUND(404, "资源不存在"),

    /**
     * 没有权限
     */
    NOT_POWER(666,"没有权限"),

    /**
     * 用户相关错误码 (1xxx)
     */
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_ALREADY_EXISTS(1002, "用户已存在"),
    USER_PASSWORD_ERROR(1003, "密码错误"),
    USER_ACCOUNT_DISABLED(1004, "账号已被禁用"),
    USER_NOT_AUTHENTICATED(1005, "用户未实名认证"),
    USER_AUTH_REJECTED(1006, "实名认证已被拒绝"),
    USER_NOT_LOGIN(1007, "用户未登录"),
    REFRESH_TOKEN_EXPIRED(1008, "刷新令牌已过期，请重新登录"),
    REFRESH_TOKEN_INVALID(1009, "刷新令牌无效，请重新登录"),

    /**
     * 商品相关错误码 (2xxx)
     */
    GOODS_NOT_FOUND(2001, "商品不存在"),
    GOODS_ALREADY_SOLD(2002, "商品已售出"),
    GOODS_OFFLINE(2003, "商品已下架"),
    GOODS_NOT_REVIEWED(2004, "商品未通过审核"),

    /**
     * 订单相关错误码 (3xxx)
     */
    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_ALREADY_PAID(3002, "订单已支付"),
    ORDER_ALREADY_CANCELLED(3003, "订单已取消"),
    ORDER_CANNOT_CANCEL(3004, "订单无法取消"),
    INSUFFICIENT_BALANCE(3005, "余额不足"),

    /**
     * 跑腿相关错误码 (4xxx)
     */
    ERRAND_NOT_FOUND(4001, "跑腿任务不存在"),
    ERRAND_ALREADY_ACCEPTED(4002, "任务已被接单"),
    ERRAND_NOT_CERTIFIED(4003, "未通过跑腿认证"),

    /**
     * 文件相关错误码 (5xxx)
     */
    FILE_UPLOAD_ERROR(5001, "文件上传失败"),
    FILE_TYPE_ERROR(5002, "文件类型不支持"),
    FILE_SIZE_ERROR(5003, "文件大小超出限制");

    /**
     * 状态码
     */
    private final Integer code;

    /**
     * 消息
     */
    private final String message;
}