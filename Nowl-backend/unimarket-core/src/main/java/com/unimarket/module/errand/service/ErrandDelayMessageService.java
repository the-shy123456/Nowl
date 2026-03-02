package com.unimarket.module.errand.service;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.module.errand.dto.ErrandAutoConfirmMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * 跑腿延时消息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrandDelayMessageService {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 发送自动确认延时消息（24小时后自动确认）
     *
     * @param taskId          任务ID
     * @param deliverTimestamp 送达时间戳
     */
    public void sendAutoConfirmMessage(Long taskId, Long deliverTimestamp) {
        ErrandAutoConfirmMessage message = new ErrandAutoConfirmMessage(taskId, deliverTimestamp);

        try {
            rocketMQTemplate.syncSendDelayTimeMills(
                    RocketMQConfig.ERRAND_AUTO_CONFIRM_TOPIC,
                    MessageBuilder.withPayload(message).build(),
                    RocketMQConfig.ERRAND_AUTO_CONFIRM_DELAY_MS
            );
            log.info("发送跑腿自动确认延时消息成功: taskId={}, 延时24小时", taskId);
        } catch (Exception e) {
            log.error("发送跑腿自动确认延时消息失败: taskId={}", taskId, e);
            // 消息发送失败不影响主流程，可以通过定时任务兜底
        }
    }
}
