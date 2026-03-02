package com.unimarket.module.order.service;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.module.order.dto.OrderAutoConfirmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 订单延时消息服务
 * 使用 RocketMQ 发送延迟消息实现自动确认收货
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDelayMessageService {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送自动确认收货延时消息（7天后自动确认）
     * 
     * @param orderId 订单ID
     * @param deliveryTimestamp 发货时间戳
     */
    public void sendAutoConfirmMessage(Long orderId, Long deliveryTimestamp) {
        OrderAutoConfirmMessage message = new OrderAutoConfirmMessage(orderId, deliveryTimestamp);
        
        // RocketMQ 5.x 支持任意延迟时间
        // 使用 syncSendDelayTimeMills 方法发送延迟消息
        rocketMQTemplate.syncSendDelayTimeMills(
            RocketMQConfig.ORDER_AUTO_CONFIRM_TOPIC,
            MessageBuilder.withPayload(message).build(),
            RocketMQConfig.AUTO_CONFIRM_DELAY_MS
        );
        
        log.info("发送自动确认收货延时消息: orderId={}, 延时7天", orderId);
    }

    /**
     * 发送测试用的短延时消息（1分钟，便于测试）
     */
    public void sendAutoConfirmMessageForTest(Long orderId, Long deliveryTimestamp) {
        OrderAutoConfirmMessage message = new OrderAutoConfirmMessage(orderId, deliveryTimestamp);
        
        rocketMQTemplate.syncSendDelayTimeMills(
            RocketMQConfig.ORDER_AUTO_CONFIRM_TOPIC,
            MessageBuilder.withPayload(message).build(),
            RocketMQConfig.TEST_DELAY_MS
        );
        
        log.info("发送自动确认收货延时消息(测试): orderId={}, 延时1分钟", orderId);
    }
}
