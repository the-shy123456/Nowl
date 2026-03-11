package com.unimarket.module.errand.listener;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.mq.ErrandAuditMessage;
import com.unimarket.module.errand.service.ErrandAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 跑腿任务审核消息监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQConfig.ERRAND_AUDIT_TOPIC,
        consumerGroup = RocketMQConfig.ERRAND_AUDIT_CONSUMER_GROUP
)
public class ErrandAuditListener implements RocketMQListener<ErrandAuditMessage> {

    private final ErrandAuditService errandAuditService;

    @Override
    public void onMessage(ErrandAuditMessage message) {
        log.info("收到跑腿审核消息: taskId={}, operationType={}", message.getTaskId(), message.getOperationType());
        try {
            errandAuditService.performAudit(message.getTaskId(), message.getOperationType());
        } catch (Exception e) {
            log.error("处理跑腿审核消息失败: taskId={}", message.getTaskId(), e);
            throw e instanceof RuntimeException runtimeException
                    ? runtimeException
                    : new IllegalStateException("跑腿审核消息处理失败", e);
        }
    }
}
