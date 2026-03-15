package com.unimarket.module.errand.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.unimarket.ai.dto.AiAuditResult;
import com.unimarket.ai.service.AiAuditService;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.constant.CacheConstants;
import com.unimarket.common.mq.ErrandAuditMessage;
import com.unimarket.common.mq.ErrandSyncMessage;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.errand.dto.ErrandAuditResult;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.errand.service.ErrandAuditService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 跑腿任务审核服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrandAuditServiceImpl implements ErrandAuditService {

    private static final String ERRAND_SYNC_TOPIC = "errand-sync-topic";

    private final ErrandTaskMapper errandTaskMapper;
    private final AiAuditService aiAuditService;
    private final NoticeService noticeService;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserInfoMapper userInfoMapper;
    private final RedisCache redisCache;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void performAudit(Long taskId, int operationType) {
        if (taskId == null) {
            return;
        }

        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            log.warn("审核跑腿任务不存在: taskId={}", taskId);
            return;
        }

        Integer currentReviewStatus = task.getReviewStatus();
        if (ReviewStatus.MANUAL_PASSED.getCode().equals(currentReviewStatus)
                || ReviewStatus.REJECTED.getCode().equals(currentReviewStatus)) {
            log.info("跑腿任务已处于终态，跳过AI审核: taskId={}, reviewStatus={}", taskId, currentReviewStatus);
            return;
        }
        if (!ReviewStatus.WAIT_REVIEW.getCode().equals(currentReviewStatus)
                && !ReviewStatus.WAIT_MANUAL.getCode().equals(currentReviewStatus)) {
            log.info("跑腿任务当前状态无需AI审核，跳过: taskId={}, reviewStatus={}", taskId, currentReviewStatus);
            return;
        }

        ErrandAuditResult auditResult = audit(task);
        if (!tryApplyAuditResult(task, auditResult)) {
            log.info("跑腿任务审核结果已被其他请求处理，跳过重复退款与通知: taskId={}", taskId);
            return;
        }
        if (auditResult.isRejected()) {
            refundReward(task);
        }

        sendAuditNotice(task, operationType, auditResult);
        sendSyncMessageAfterCommit(task, operationType, auditResult);

        log.info("跑腿AI审核完成: taskId={}, reviewStatus={}", taskId, task.getReviewStatus());
    }

    private void sendAuditNotice(ErrandTask task, int operationType, ErrandAuditResult auditResult) {
        String title;
        String content;
        if (auditResult.isPassed()) {
            title = "跑腿任务审核通过";
            String actionText = operationType == ErrandAuditMessage.TYPE_CREATE ? "发布成功" : "更新成功";
            content = "您的跑腿任务【" + task.getTitle() + "】已通过审核并" + actionText + "。";
        } else if (auditResult.isWaitManual()) {
            title = "跑腿任务待人工复核";
            content = "您的跑腿任务【" + task.getTitle() + "】需要人工复核，暂不可见。原因：" + task.getAuditReason();
        } else {
            title = "跑腿任务审核未通过";
            content = "您的跑腿任务【" + task.getTitle() + "】未通过审核，悬赏金额已退回余额。原因：" + task.getAuditReason();
        }

        noticeService.sendNotice(
                task.getPublisherId(),
                title,
                content,
                NoticeType.TRADE.getCode(),
                task.getTaskId()
        );
    }

    private void sendSyncMessageAfterCommit(ErrandTask task, int operationType, ErrandAuditResult auditResult) {
        Long taskId = task.getTaskId();
        boolean cancelled = ErrandStatus.CANCELLED.getCode().equals(task.getTaskStatus());

        ErrandSyncMessage message;
        String actionName;
        if (auditResult.isPassed() && !cancelled) {
            message = operationType == ErrandAuditMessage.TYPE_CREATE
                    ? ErrandSyncMessage.createMessage(taskId)
                    : ErrandSyncMessage.updateMessage(taskId);
            actionName = "同步";
        } else {
            message = ErrandSyncMessage.deleteMessage(taskId);
            actionName = "下线";
        }

        Runnable sendTask = () -> {
            try {
                rocketMQTemplate.convertAndSend(ERRAND_SYNC_TOPIC, message);
                log.info("发送跑腿任务{}消息成功: taskId={}, type={}", actionName, taskId, message.getType());
            } catch (Exception e) {
                log.error("发送跑腿任务{}消息失败: taskId={}, type={}", actionName, taskId, message.getType(), e);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
            return;
        }
        sendTask.run();
    }

    private ErrandAuditResult audit(ErrandTask task) {
        // 1. 文本审核
        AiAuditResult textResult = aiAuditService.auditText(buildAuditText(task));
        if (isHighRisk(textResult)) {
            String reason = defaultReason(textResult.getReason(), "跑腿文本内容存在高风险，审核未通过");
            return new ErrandAuditResult(ReviewStatus.REJECTED.getCode(), reason);
        }
        if (isMediumRisk(textResult)) {
            String reason = defaultReason(textResult.getReason(), "跑腿文本内容需要人工复核");
            return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), reason);
        }

        // 2. 图片审核：为降低误杀，图片高风险也进入人工复核
        List<String> imageUrls = parseImages(task.getImageList());
        if (imageUrls == null) {
            return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), "跑腿图片数据格式异常，需人工复核");
        }
        for (String imageUrl : imageUrls) {
            AiAuditResult imageResult = aiAuditService.auditImage(imageUrl);
            if (isHighRisk(imageResult)) {
                String reason = defaultReason(imageResult.getReason(), "跑腿图片存在高风险，需人工复核");
                return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), reason);
            }
            if (isMediumRisk(imageResult)) {
                String reason = defaultReason(imageResult.getReason(), "跑腿图片识别不确定，需人工复核");
                return new ErrandAuditResult(ReviewStatus.WAIT_MANUAL.getCode(), reason);
            }
        }

        return new ErrandAuditResult(ReviewStatus.AI_PASSED.getCode(), null);
    }

    private String buildAuditText(ErrandTask task) {
        StringBuilder builder = new StringBuilder();
        append(builder, task.getTitle());
        append(builder, task.getDescription());
        append(builder, task.getTaskContent());
        append(builder, task.getPickupAddress());
        append(builder, task.getDeliveryAddress());
        append(builder, task.getRemark());
        return builder.toString().trim();
    }

    private void append(StringBuilder builder, String value) {
        if (StrUtil.isNotBlank(value)) {
            builder.append(value).append('\n');
        }
    }

    private List<String> parseImages(String imageList) {
        if (StrUtil.isBlank(imageList)) {
            return List.of();
        }
        try {
            List<String> images = JSONUtil.toList(imageList, String.class);
            List<String> result = new ArrayList<>();
            for (String image : images) {
                if (StrUtil.isNotBlank(image)) {
                    result.add(image);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析跑腿图片列表失败: {}", imageList);
            return null;
        }
    }

    private boolean isHighRisk(AiAuditResult result) {
        return "high".equals(result.getRiskLevel()) || !result.isSafe();
    }

    private boolean isMediumRisk(AiAuditResult result) {
        return "medium".equals(result.getRiskLevel());
    }

    private String defaultReason(String reason, String fallback) {
        return StrUtil.isBlank(reason) ? fallback : reason;
    }

    private boolean tryApplyAuditResult(ErrandTask task, ErrandAuditResult auditResult) {
        UpdateWrapper<ErrandTask> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("task_id", task.getTaskId())
                .in("review_status",
                        ReviewStatus.WAIT_REVIEW.getCode(),
                        ReviewStatus.WAIT_MANUAL.getCode())
                .set("review_status", auditResult.getReviewStatus())
                .set("audit_reason", auditResult.getReason());
        int updated = errandTaskMapper.update(null, updateWrapper);
        if (updated <= 0) {
            return false;
        }
        task.setReviewStatus(auditResult.getReviewStatus());
        task.setAuditReason(auditResult.getReason());
        // 审核结果变更会影响详情可见性：主动失效详情缓存，避免旧数据被继续读取
        redisCache.delete(CacheConstants.ERRAND_DETAIL + "guest:" + task.getTaskId());
        if (StrUtil.isNotBlank(task.getSchoolCode())) {
            redisCache.delete(CacheConstants.ERRAND_DETAIL + task.getSchoolCode().trim() + ":" + task.getTaskId());
        }
        return true;
    }

    private void refundReward(ErrandTask task) {
        BigDecimal reward = task.getReward();
        if (reward == null || reward.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        UserInfo publisher = userInfoMapper.selectById(task.getPublisherId());
        if (publisher == null) {
            throw new IllegalStateException("跑腿任务发布者不存在，无法退还审核失败托管金额");
        }
        publisher.setMoney(publisher.getMoney().add(reward));
        int updated = userInfoMapper.updateById(publisher);
        if (updated <= 0) {
            throw new IllegalStateException("退还跑腿任务审核失败托管金额失败");
        }
        log.info("跑腿任务审核未通过，已退还托管金额: taskId={}, publisherId={}, reward={}",
                task.getTaskId(), task.getPublisherId(), reward);
    }
}



