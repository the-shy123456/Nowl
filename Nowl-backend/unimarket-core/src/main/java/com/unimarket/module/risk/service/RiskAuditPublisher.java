package com.unimarket.module.risk.service;

import com.alibaba.fastjson2.JSON;
import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.module.risk.dto.RiskAuditMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 风控审计消息投递器。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAuditPublisher {

    private static final String FALLBACK_QUEUE_KEY = "risk:audit:fallback";
    private static final int REPLAY_BATCH_SIZE = 100;
    private static final int MAX_RETRY_COUNT = 3;
    private static final long SEND_TIMEOUT_MS = 1_000L;

    private final RocketMQTemplate rocketMQTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final RiskAuditBatchBuffer riskAuditBatchBuffer;

    public void publish(RiskAuditMessage message) {
        if (message == null) {
            return;
        }
        try {
            rocketMQTemplate.syncSend(RocketMQConfig.RISK_AUDIT_TOPIC, message, SEND_TIMEOUT_MS);
            return;
        } catch (Exception ex) {
            log.warn("风控审计消息发送失败，准备写入 Redis 兜底队列，eventId={}", eventIdOf(message), ex);
        }

        if (enqueueFallback(message)) {
            return;
        }

        riskAuditBatchBuffer.enqueue(message);
        log.error("Redis 兜底队列写入失败，已直接回退为本地批量落库，eventId={}", eventIdOf(message));
    }

    @Scheduled(fixedDelay = 5000L)
    public void replayFallbackQueue() {
        for (int i = 0; i < REPLAY_BATCH_SIZE; i++) {
            String payload = stringRedisTemplate.opsForList().rightPop(FALLBACK_QUEUE_KEY);
            if (payload == null || payload.isBlank()) {
                return;
            }

            RiskAuditMessage message;
            try {
                message = JSON.parseObject(payload, RiskAuditMessage.class);
            } catch (Exception ex) {
                log.error("解析风控兜底消息失败，payload={}", payload, ex);
                continue;
            }

            try {
                rocketMQTemplate.syncSend(RocketMQConfig.RISK_AUDIT_TOPIC, message, SEND_TIMEOUT_MS);
            } catch (Exception ex) {
                int retryCount = message.getMqRetryCount() == null ? 0 : message.getMqRetryCount();
                if (retryCount >= MAX_RETRY_COUNT) {
                    riskAuditBatchBuffer.enqueue(message);
                    log.error("风控审计消息重试仍失败，已回退为本地批量落库，eventId={}", eventIdOf(message), ex);
                    continue;
                }
                message.setMqRetryCount(retryCount + 1);
                if (!enqueueFallback(message)) {
                    riskAuditBatchBuffer.enqueue(message);
                    log.error("风控审计消息重放失败且 Redis 回写失败，已回退为本地批量落库，eventId={}", eventIdOf(message), ex);
                }
                return;
            }
        }
    }

    private boolean enqueueFallback(RiskAuditMessage message) {
        try {
            int retryCount = message.getMqRetryCount() == null ? 0 : message.getMqRetryCount();
            message.setMqRetryCount(retryCount + 1);
            stringRedisTemplate.opsForList().leftPush(FALLBACK_QUEUE_KEY, JSON.toJSONString(message));
            return true;
        } catch (Exception ex) {
            log.error("写入风控兜底队列失败，eventId={}", eventIdOf(message), ex);
            return false;
        }
    }

    private Long eventIdOf(RiskAuditMessage message) {
        return message == null || message.getEvent() == null ? null : message.getEvent().getEventId();
    }
}
