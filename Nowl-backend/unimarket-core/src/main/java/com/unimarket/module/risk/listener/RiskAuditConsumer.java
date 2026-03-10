package com.unimarket.module.risk.listener;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.module.risk.dto.RiskAuditMessage;
import com.unimarket.module.risk.service.RiskAuditBatchBuffer;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 风控审计消息消费者。
 */
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQConfig.RISK_AUDIT_TOPIC,
        consumerGroup = RocketMQConfig.RISK_AUDIT_CONSUMER_GROUP
)
public class RiskAuditConsumer implements RocketMQListener<RiskAuditMessage> {

    private final RiskAuditBatchBuffer riskAuditBatchBuffer;

    @Override
    public void onMessage(RiskAuditMessage message) {
        riskAuditBatchBuffer.enqueue(message);
    }
}
