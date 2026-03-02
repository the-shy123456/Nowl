package com.unimarket.module.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.enums.DisputeStatus;
import com.unimarket.common.enums.DisputeTargetType;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.RefundStatus;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.dto.OrderAutoConfirmMessage;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 订单自动确认消息监听器
 * 使用 RocketMQ 监听延迟消息，处理自动确认收货
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
    topic = RocketMQConfig.ORDER_AUTO_CONFIRM_TOPIC,
    consumerGroup = RocketMQConfig.ORDER_AUTO_CONFIRM_CONSUMER_GROUP
)
public class OrderAutoConfirmListener implements RocketMQListener<OrderAutoConfirmMessage> {

    private final OrderInfoMapper orderInfoMapper;
    private final DisputeRecordMapper disputeRecordMapper;
    private final UserInfoMapper userInfoMapper;
    private final NoticeService noticeService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(OrderAutoConfirmMessage message) {
        Long orderId = message.getOrderId();
        log.info("收到自动确认收货消息: orderId={}", orderId);

        String lockKey = "order:lock:lifecycle:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!acquired) {
                log.debug("订单自动确认跳过，订单正在处理: orderId={}", orderId);
                return;
            }

            // 1. 查询订单
            OrderInfo order = orderInfoMapper.selectById(orderId);
            if (order == null) {
                log.warn("订单不存在，跳过自动确认: orderId={}", orderId);
                return;
            }

            // 2. 检查订单状态（只有待收货状态才处理）
            if (!OrderStatus.PENDING_RECEIVE.getCode().equals(order.getOrderStatus())) {
                log.info("订单状态不是待收货，跳过自动确认: orderId={}, status={}", orderId, order.getOrderStatus());
                return;
            }
            if (RefundStatus.PENDING.getCode().equals(order.getRefundStatus())) {
                log.info("订单存在退款处理中，跳过自动确认: orderId={}", orderId);
                return;
            }
            if (hasActiveOrderDispute(orderId)) {
                log.info("订单存在进行中纠纷，跳过自动确认: orderId={}", orderId);
                return;
            }

            // 3. 校验发货时间（防止消息重复消费导致的问题）
            if (order.getDeliveryTime() != null && message.getDeliveryTimestamp() != null) {
                long orderDeliveryTs = order.getDeliveryTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (!message.getDeliveryTimestamp().equals(orderDeliveryTs)) {
                    log.info("发货时间不匹配，可能是重新发货的订单，跳过: orderId={}", orderId);
                    return;
                }
            }

            // 4. 资金结算：转入卖家账户
            UserInfo seller = userInfoMapper.selectById(order.getSellerId());
            if (seller != null) {
                seller.setMoney(seller.getMoney().add(order.getTotalAmount()));
                userInfoMapper.updateById(seller);
                log.info("资金已转入卖家账户: sellerId={}, amount={}", seller.getUserId(), order.getTotalAmount());
            }

            // 5. 更新订单状态
            order.setOrderStatus(OrderStatus.COMPLETED.getCode()); // 已完成
            order.setReceiveTime(LocalDateTime.now());
            orderInfoMapper.updateById(order);

            log.info("订单自动确认收货成功: orderId={}, orderNo={}", orderId, order.getOrderNo());

            // 6. 发送通知
            noticeService.sendNotice(order.getBuyerId(), "自动收货通知", 
                "您的订单 [" + order.getOrderNo() + "] 因超时未确认已自动收货。", NoticeType.TRADE.getCode());
            noticeService.sendNotice(order.getSellerId(), "交易完成", 
                "订单 [" + order.getOrderNo() + "] 已自动确认收货，资金已入账。", NoticeType.TRADE.getCode());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("订单自动确认被中断: orderId={}", orderId);
        } catch (Exception e) {
            log.error("订单自动确认收货失败: orderId={}", orderId, e);
            throw e; // 重新抛出异常，让消息重试
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean hasActiveOrderDispute(Long orderId) {
        LambdaQueryWrapper<DisputeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DisputeRecord::getContentId, orderId)
                .eq(DisputeRecord::getTargetType, DisputeTargetType.ORDER.getCode())
                .in(DisputeRecord::getHandleStatus, DisputeStatus.PENDING.getCode(), DisputeStatus.PROCESSING.getCode());
        return disputeRecordMapper.selectCount(wrapper) > 0;
    }
}
