package com.unimarket.module.errand.listener;

import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.module.errand.dto.ErrandAutoConfirmMessage;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * 跑腿自动确认消息监听器
 * 监听延时消息，24小时后自动确认完成
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = RocketMQConfig.ERRAND_AUTO_CONFIRM_TOPIC,
        consumerGroup = RocketMQConfig.ERRAND_AUTO_CONFIRM_CONSUMER_GROUP
)
public class ErrandAutoConfirmListener implements RocketMQListener<ErrandAutoConfirmMessage> {

    private final ErrandTaskMapper errandTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final NoticeService noticeService;
    private final RedissonClient redissonClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onMessage(ErrandAutoConfirmMessage message) {
        Long taskId = message.getTaskId();
        log.info("收到跑腿自动确认消息: taskId={}", taskId);

        String lockKey = "errand:lock:lifecycle:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!acquired) {
                log.debug("跑腿自动确认跳过，任务正在处理: taskId={}", taskId);
                return;
            }

            ErrandTask task = errandTaskMapper.selectById(taskId);
            if (task == null) {
                log.warn("跑腿任务不存在: taskId={}", taskId);
                return;
            }

            // 只处理待确认状态的任务
            if (!ErrandStatus.PENDING_CONFIRM.getCode().equals(task.getTaskStatus())) {
                log.info("任务状态不是待确认，跳过自动确认: taskId={}, status={}", taskId, task.getTaskStatus());
                return;
            }

            // 校验送达时间防止重复消费
            if (task.getDeliverTime() != null && message.getDeliverTimestamp() != null) {
                long taskDeliverTs = task.getDeliverTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (!message.getDeliverTimestamp().equals(taskDeliverTs)) {
                    log.info("送达时间不匹配，跳过自动确认: taskId={}", taskId);
                    return;
                }
            }

            // 结算佣金给接单人
            if (task.getAcceptorId() != null) {
                UserInfo acceptor = userInfoMapper.selectById(task.getAcceptorId());
                if (acceptor != null) {
                    acceptor.setMoney(acceptor.getMoney().add(task.getReward()));
                    userInfoMapper.updateById(acceptor);
                    log.info("佣金已自动结算给接单人: acceptorId={}, amount={}", acceptor.getUserId(), task.getReward());
                }
            }

            // 更新任务状态为已完成
            task.setTaskStatus(ErrandStatus.COMPLETED.getCode());
            task.setConfirmTime(LocalDateTime.now());
            errandTaskMapper.updateById(task);

            log.info("跑腿任务自动确认完成: taskId={}", taskId);

            // 发送通知给发布者
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿自动确认",
                    "您的跑腿任务 [" + task.getTitle() + "] 因超时未确认已自动完成。",
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            // 发送通知给接单人
            if (task.getAcceptorId() != null) {
                noticeService.sendNotice(
                        task.getAcceptorId(),
                        "佣金到账",
                        "跑腿任务 [" + task.getTitle() + "] 已自动确认，佣金 ¥" + task.getReward() + " 已到账。",
                        NoticeType.TRADE.getCode(),
                        taskId
                );
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("跑腿自动确认被中断: taskId={}", taskId);
        } catch (Exception e) {
            log.error("跑腿自动确认失败: taskId={}", taskId, e);
            throw e;
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
