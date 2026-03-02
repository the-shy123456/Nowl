package com.unimarket.common.constant;

/**
 * 系统常量类
 * 注意：敏感配置已移至 application.yml，通过 SystemProperties 读取
 */
public class SystemConstants {

    /**
     * 允许上传的文件类型
     */
    public static final String[] ALLOWED_FILE_TYPES = {"jpg", "jpeg", "png", "gif", "webp"};

    /**
     * 文件大小限制（10MB）
     */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * 初始信用分
     */
    public static final int INITIAL_CREDIT_SCORE = 100;

    /**
     * 初始余额
     */
    public static final double INITIAL_BALANCE = 0.00;


    /**
     * 创建商品-用于异步审核后发送mq消息时判断
     */
    public static final int CREATE_GOODS = 1;

    /**
     * 更新商品-同上
     */
    public static final int UPDATE_GOODS = 2;


}
