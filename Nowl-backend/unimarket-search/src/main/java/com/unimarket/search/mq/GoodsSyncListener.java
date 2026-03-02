package com.unimarket.search.mq;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.mq.GoodsSyncMessage;
import com.unimarket.search.service.SearchSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 商品同步消息监听器
 * 监听商品变更消息，同步到ES
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMQConfig.GOODS_SYNC_TOPIC,
    consumerGroup = RocketMQConfig.GOODS_SYNC_CONSUMER_GROUP
)
public class GoodsSyncListener implements RocketMQListener<GoodsSyncMessage> {

    private final SearchSyncService searchSyncService;

    @Override
    public void onMessage(GoodsSyncMessage message) {
        log.info("收到商品同步消息: type={}, productId={}", message.getType(), message.getProductId());

        try {
            switch (message.getType()) {
                case CREATE:
                case UPDATE:
                    searchSyncService.syncGoods(message.getProductId());
                    break;
                case DELETE:
                    searchSyncService.deleteGoods(message.getProductId());
                    break;
                case UPDATE_HOT_SCORE:
                    searchSyncService.updateHotScore(message.getProductId(), message.getHotScore());
                    break;
                case UPDATE_VIEW_COUNT:
                    searchSyncService.updateViewCount(message.getProductId(), message.getViewCount());
                    break;
                default:
                    log.warn("未知的同步类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("处理商品同步消息失败", e);
        }
    }
}
