package com.unimarket.module.goods.listener;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.mq.GoodsAuditMessage;
import com.unimarket.module.goods.service.GoodsAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 商品审核消息监听器
 * 通过 MQ 异步处理商品审核
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMQConfig.GOODS_AUDIT_TOPIC,
    consumerGroup = RocketMQConfig.GOODS_AUDIT_CONSUMER_GROUP
)
public class GoodsAuditListener implements RocketMQListener<GoodsAuditMessage> {

    private final GoodsAuditService goodsAuditService;

    @Override
    public void onMessage(GoodsAuditMessage message) {
        log.info("收到商品审核消息: productId={}, operationType={}",
                message.getProductId(), message.getOperationType());
        try {
            goodsAuditService.performAudit(message.getProductId(), message.getOperationType());
        } catch (Exception e) {
            log.error("处理商品审核消息失败: productId={}", message.getProductId(), e);
        }
    }
}
