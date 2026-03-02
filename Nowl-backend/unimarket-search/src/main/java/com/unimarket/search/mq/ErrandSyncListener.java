package com.unimarket.search.mq;

import com.unimarket.common.mq.ErrandSyncMessage;
import com.unimarket.search.service.ErrandSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 跑腿任务同步消息监听器
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = "errand-sync-topic",
    consumerGroup = "errand-sync-consumer-group"
)
public class ErrandSyncListener implements RocketMQListener<ErrandSyncMessage> {

    private final ErrandSearchService errandSearchService;

    @Override
    public void onMessage(ErrandSyncMessage message) {
        log.info("收到跑腿同步消息: type={}, taskId={}", message.getType(), message.getTaskId());

        try {
            switch (message.getType()) {
                case CREATE:
                case UPDATE:
                    errandSearchService.syncErrand(message.getTaskId());
                    break;
                case DELETE:
                    errandSearchService.deleteErrand(message.getTaskId());
                    break;
                default:
                    log.warn("未知的同步类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("处理跑腿同步消息失败", e);
        }
    }
}
