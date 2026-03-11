package com.unimarket.common.config;

import org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * RocketMQ 配置类
 * 
 * RocketMQ 延迟等级（内置）：
 * 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
 * 
 * 对于7天自动确认，使用自定义方式：发送多条消息链式延迟
 * 或使用 RocketMQ 5.x 的任意延迟时间功能
 */
@Configuration
@Import(RocketMQAutoConfiguration.class)
public class RocketMQConfig {

    /**
     * 订单自动确认 Topic
     */
    public static final String ORDER_AUTO_CONFIRM_TOPIC = "order-auto-confirm-topic";

    /**
     * 消费者组
     */
    public static final String ORDER_AUTO_CONFIRM_CONSUMER_GROUP = "order-auto-confirm-consumer-group";
    
    /**
     * 自动确认延迟时间（7天，单位毫秒）
     * RocketMQ 5.x 支持任意延迟时间
     */
    public static final long AUTO_CONFIRM_DELAY_MS = 7 * 24 * 60 * 60 * 1000L;
    
    /**
     * 测试用延迟时间（1分钟）
     */
    public static final long TEST_DELAY_MS = 60 * 1000L;

    /**
     * 跑腿自动确认 Topic
     */
    public static final String ERRAND_AUTO_CONFIRM_TOPIC = "errand-auto-confirm-topic";

    /**
     * 跑腿自动确认消费者组
     */
    public static final String ERRAND_AUTO_CONFIRM_CONSUMER_GROUP = "errand-auto-confirm-consumer-group";

    /**
     * 跑腿自动确认延迟时间（24小时，单位毫秒）
     */
    public static final long ERRAND_AUTO_CONFIRM_DELAY_MS = 24 * 60 * 60 * 1000L;

    /**
     * 跑腿审核 Topic
     */
    public static final String ERRAND_AUDIT_TOPIC = "errand-audit-topic";

    /**
     * 跑腿审核消费者组
     */
    public static final String ERRAND_AUDIT_CONSUMER_GROUP = "errand-audit-consumer-group";

    /**
     * 商品审核 Topic
     */
    public static final String GOODS_AUDIT_TOPIC = "goods-audit-topic";

    /**
     * 商品审核消费者组
     */
    public static final String GOODS_AUDIT_CONSUMER_GROUP = "goods-audit-consumer-group";

    /**
     * 商品同步 Topic
     */
    public static final String GOODS_SYNC_TOPIC = "goods-sync-topic";

    /**
     * 商品同步消费者组
     */
    public static final String GOODS_SYNC_CONSUMER_GROUP = "goods-sync-consumer-group";
    /**
     * 风控审计 Topic
     */
    public static final String RISK_AUDIT_TOPIC = "risk-audit-topic";

    /**
     * 风控审计消费者组
     */
    public static final String RISK_AUDIT_CONSUMER_GROUP = "risk-audit-consumer-group";
}


