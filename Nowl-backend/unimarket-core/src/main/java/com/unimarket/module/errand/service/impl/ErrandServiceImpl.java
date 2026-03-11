package com.unimarket.module.errand.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.mq.ErrandSyncMessage;
import com.unimarket.common.result.PageQuery;
import com.unimarket.common.result.ResultCode;
import com.unimarket.module.errand.dto.ErrandAuditResult;
import com.unimarket.module.errand.dto.ErrandPublishDTO;
import com.unimarket.module.errand.dto.ErrandQueryDTO;
import com.unimarket.module.errand.dto.LocationUploadDTO;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.errand.service.ErrandAuditService;
import com.unimarket.module.errand.service.ErrandDelayMessageService;
import com.unimarket.module.errand.service.ErrandService;
import com.unimarket.module.errand.vo.ErrandVO;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.enums.RiskEventType;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 跑腿任务Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErrandServiceImpl implements ErrandService {

    private static final String ERRAND_SYNC_TOPIC = "errand-sync-topic";

    private final ErrandTaskMapper errandTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final SchoolInfoMapper schoolInfoMapper;
    private final NoticeService noticeService;
    private final RedissonClient redissonClient;
    private final ErrandDelayMessageService errandDelayMessageService;
    private final RocketMQTemplate rocketMQTemplate;
    private final RiskControlService riskControlService;
    private final ErrandAuditService errandAuditService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishErrand(Long userId, ErrandPublishDTO dto) {
        // 权限校验已在Controller层通过@PreAuthorize完成
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 校区合法性校验
        LambdaQueryWrapper<SchoolInfo> schoolWrapper = new LambdaQueryWrapper<>();
        schoolWrapper.eq(SchoolInfo::getSchoolCode, dto.getSchoolCode())
                .eq(SchoolInfo::getCampusCode, dto.getCampusCode())
                .eq(SchoolInfo::getStatus, 1);
        SchoolInfo schoolInfo = schoolInfoMapper.selectOne(schoolWrapper);
        if (schoolInfo == null) {
            throw new BusinessException("校区信息不存在或已停用");
        }

        Map<String, Object> features = new HashMap<>();
        features.put("reward", dto.getReward());
        features.put("titleLength", dto.getTitle() == null ? 0 : dto.getTitle().length());

        Map<String, Object> rawPayload = new HashMap<>();
        rawPayload.put("title", dto.getTitle());
        rawPayload.put("taskContent", dto.getTaskContent());
        rawPayload.put("pickupAddress", dto.getPickupAddress());
        rawPayload.put("deliveryAddress", dto.getDeliveryAddress());
        rawPayload.put("reward", dto.getReward());
        rawPayload.put("publisherId", userId);

        riskControlService.assertAllowed(RiskContext.builder()
                .eventType(RiskEventType.ERRAND_PUBLISH)
                .userId(userId)
                .subjectId(String.valueOf(userId))
                .schoolCode(dto.getSchoolCode())
                .campusCode(dto.getCampusCode())
                .features(features)
                .rawPayload(rawPayload)
                .build());

        ErrandAuditResult auditResult = errandAuditService.audit(dto);
        if (auditResult.isRejected()) {
            throw new BusinessException("跑腿任务审核未通过：" + auditResult.getReason());
        }

        ErrandTask task = BeanUtil.copyProperties(dto, ErrandTask.class);

        // 检查余额
        if (user.getMoney().compareTo(task.getReward()) < 0) {
            throw new BusinessException("余额不足，无法发布悬赏任务");
        }

        // 扣除余额（资金托管）
        user.setMoney(user.getMoney().subtract(task.getReward()));
        userInfoMapper.updateById(user);

        task.setPublisherId(userId);
        task.setTaskStatus(ErrandStatus.PENDING.getCode());
        task.setSchoolCode(dto.getSchoolCode());
        task.setCampusCode(dto.getCampusCode());
        task.setPickupLatitude(null);
        task.setPickupLongitude(null);
        task.setDeliveryLatitude(null);
        task.setDeliveryLongitude(null);
        task.setReviewStatus(auditResult.getReviewStatus());
        task.setAuditReason(auditResult.getReason());

        if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
            try {
                task.setDeadline(LocalDateTime.parse(dto.getDeadline(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                // 尝试其他格式
                try {
                    task.setDeadline(LocalDateTime.parse(dto.getDeadline(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
                } catch (Exception ex) {
                    log.warn("解析截止时间失败: {}", dto.getDeadline());
                }
            }
        }

        errandTaskMapper.insert(task);
        log.info("发布跑腿任务成功: taskId={}, userId={}, reward={}, reviewStatus={}",
                task.getTaskId(), userId, task.getReward(), task.getReviewStatus());

        if (isReviewPassed(task.getReviewStatus())) {
            sendErrandSyncAfterCommit(task.getTaskId(), ErrandSyncMessage.createMessage(task.getTaskId()), "发布");
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务审核通过",
                    "您的跑腿任务【" + task.getTitle() + "】已通过审核并发布成功。",
                    NoticeType.TRADE.getCode(),
                    task.getTaskId()
            );
        } else if (ReviewStatus.WAIT_MANUAL.getCode().equals(task.getReviewStatus())) {
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务待人工复核",
                    "您的跑腿任务【" + task.getTitle() + "】需要人工复核，暂不可见。原因：" + task.getAuditReason(),
                    NoticeType.TRADE.getCode(),
                    task.getTaskId()
            );
        } else if (ReviewStatus.REJECTED.getCode().equals(task.getReviewStatus())) {
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务审核未通过",
                    "您的跑腿任务【" + task.getTitle() + "】未通过审核。原因：" + task.getAuditReason(),
                    NoticeType.TRADE.getCode(),
                    task.getTaskId()
            );
        }
    }

    @Override
    public Page<ErrandVO> getErrandList(ErrandQueryDTO dto) {
        Page<ErrandTask> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ErrandTask> wrapper = new LambdaQueryWrapper<>();

        if (dto.getTaskStatus() != null) {
            wrapper.eq(ErrandTask::getTaskStatus, dto.getTaskStatus());
        }
        if (dto.getKeyword() != null && !dto.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(ErrandTask::getTitle, dto.getKeyword())
                    .or().like(ErrandTask::getDescription, dto.getKeyword()));
        }
        if (StrUtil.isNotBlank(dto.getSchoolCode())) {
            wrapper.eq(ErrandTask::getSchoolCode, dto.getSchoolCode());
        }
        if (StrUtil.isNotBlank(dto.getCampusCode())) {
            wrapper.eq(ErrandTask::getCampusCode, dto.getCampusCode());
        }
        wrapper.in(ErrandTask::getReviewStatus, ReviewStatus.AI_PASSED.getCode(), ReviewStatus.MANUAL_PASSED.getCode());
        wrapper.orderByDesc(ErrandTask::getCreateTime);

        Page<ErrandTask> taskPage = errandTaskMapper.selectPage(page, wrapper);

        Page<ErrandVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(convertToVOList(taskPage.getRecords()));

        return voPage;
    }

    @Override
    public ErrandVO getErrandDetail(Long taskId) {
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.PARAM_IS_INVALID);
        }
        if (!isReviewPassed(task.getReviewStatus())) {
            Long currentUserId = UserContextHolder.getUserId();
            if (currentUserId == null || !currentUserId.equals(task.getPublisherId())) {
                throw new BusinessException("任务不存在或未通过审核");
            }
        }
        return convertToVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateErrand(Long userId, Long taskId, ErrandPublishDTO dto) {
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (!task.getPublisherId().equals(userId)) {
            throw new BusinessException("无权限修改该任务");
        }
        if (!ErrandStatus.PENDING.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("仅待接单状态可修改");
        }

        ErrandAuditResult auditResult = errandAuditService.audit(dto);
        if (auditResult.isRejected()) {
            throw new BusinessException("跑腿任务审核未通过：" + auditResult.getReason());
        }

        UserInfo publisher = userInfoMapper.selectById(userId);
        if (publisher == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (dto.getReward() == null || dto.getReward().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BusinessException("报酬金额不合法");
        }

        java.math.BigDecimal oldReward = task.getReward();
        java.math.BigDecimal newReward = dto.getReward();
        int cmp = newReward.compareTo(oldReward);
        if (cmp > 0) {
            java.math.BigDecimal diff = newReward.subtract(oldReward);
            if (publisher.getMoney().compareTo(diff) < 0) {
                throw new BusinessException("余额不足，无法提高悬赏金额");
            }
            publisher.setMoney(publisher.getMoney().subtract(diff));
            userInfoMapper.updateById(publisher);
        } else if (cmp < 0) {
            java.math.BigDecimal diff = oldReward.subtract(newReward);
            publisher.setMoney(publisher.getMoney().add(diff));
            userInfoMapper.updateById(publisher);
        }

        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setTaskContent(dto.getTaskContent());
        task.setImageList(dto.getImageList());
        task.setPickupAddress(dto.getPickupAddress());
        task.setPickupLatitude(null);
        task.setPickupLongitude(null);
        task.setDeliveryAddress(dto.getDeliveryAddress());
        task.setDeliveryLatitude(null);
        task.setDeliveryLongitude(null);
        task.setReward(dto.getReward());
        task.setRemark(dto.getRemark());
        task.setReviewStatus(auditResult.getReviewStatus());
        task.setAuditReason(auditResult.getReason());

        if (dto.getDeadline() != null && !dto.getDeadline().isEmpty()) {
            try {
                task.setDeadline(LocalDateTime.parse(dto.getDeadline(),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception e) {
                try {
                    task.setDeadline(LocalDateTime.parse(dto.getDeadline(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
                } catch (Exception ex) {
                    log.warn("解析截止时间失败: {}", dto.getDeadline());
                }
            }
        } else {
            task.setDeadline(null);
        }

        errandTaskMapper.updateById(task);
        log.info("跑腿任务修改成功: taskId={}, userId={}, reviewStatus={}", taskId, userId, task.getReviewStatus());

        if (isReviewPassed(task.getReviewStatus())) {
            sendErrandSyncAfterCommit(taskId, ErrandSyncMessage.updateMessage(taskId), "修改");
        } else {
            sendErrandSyncAfterCommit(taskId, ErrandSyncMessage.deleteMessage(taskId), "下线");
            if (ReviewStatus.WAIT_MANUAL.getCode().equals(task.getReviewStatus())) {
                noticeService.sendNotice(
                        task.getPublisherId(),
                        "跑腿任务待人工复核",
                        "您修改的跑腿任务【" + task.getTitle() + "】需要人工复核，暂不可见。原因：" + task.getAuditReason(),
                        NoticeType.TRADE.getCode(),
                        task.getTaskId()
                );
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptErrand(Long userId, Long taskId) {
        // 分布式锁防止并发接单
        String lockKey = "errand:lock:lifecycle:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            // 权限校验已在Controller层通过@PreAuthorize完成
            UserInfo user = userInfoMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException(ResultCode.USER_NOT_FOUND);
            }

            ErrandTask task = errandTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在");
            }
            if (!ErrandStatus.PENDING.getCode().equals(task.getTaskStatus())) {
                throw new BusinessException("任务已被接单或已结束");
            }
            if (!isReviewPassed(task.getReviewStatus())) {
                throw new BusinessException("任务尚未通过审核，暂不可接单");
            }
            if (task.getPublisherId().equals(userId)) {
                throw new BusinessException("不能接自己发布的任务");
            }

            Map<String, Object> features = new HashMap<>();
            features.put("taskId", taskId);
            features.put("reward", task.getReward());

            Map<String, Object> rawPayload = new HashMap<>();
            rawPayload.put("taskId", taskId);
            rawPayload.put("acceptorId", userId);
            rawPayload.put("publisherId", task.getPublisherId());
            rawPayload.put("reward", task.getReward());
            rawPayload.put("title", task.getTitle());

            riskControlService.assertAllowed(RiskContext.builder()
                    .eventType(RiskEventType.ERRAND_ACCEPT)
                    .userId(userId)
                    .subjectId(String.valueOf(userId))
                    .schoolCode(task.getSchoolCode())
                    .campusCode(task.getCampusCode())
                    .features(features)
                    .rawPayload(rawPayload)
                    .build());

            task.setAcceptorId(userId);
            task.setTaskStatus(ErrandStatus.IN_PROGRESS.getCode());
            task.setAcceptTime(LocalDateTime.now());
            errandTaskMapper.updateById(task);

            log.info("接单成功: taskId={}, acceptorId={}", taskId, userId);

            // 通知发布者
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务已被接单",
                    "您的任务 [" + task.getTitle() + "] 已被接单，跑腿员正在为您服务。",
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            // 同步到ES
            sendErrandSyncAfterCommit(taskId, ErrandSyncMessage.updateMessage(taskId), "接单");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("接单过程中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deliverErrand(Long taskId, String evidenceImage) {
        String lockKey = "errand:lock:lifecycle:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            ErrandTask task = errandTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在");
            }

            // 权限验证已由 @PreAuthorize 在Controller层完成
            if (!ErrandStatus.IN_PROGRESS.getCode().equals(task.getTaskStatus())) {
                throw new BusinessException("当前任务状态不可送达");
            }

            if (task.getAcceptorId() == null) {
                throw new BusinessException("任务未被接单，无法送达");
            }

            if (evidenceImage == null || evidenceImage.isEmpty()) {
                throw new BusinessException("请上传送达凭证图片");
            }

            // 更新状态为待确认
            task.setTaskStatus(ErrandStatus.PENDING_CONFIRM.getCode());
            task.setEvidenceImage(evidenceImage);
            task.setDeliverTime(LocalDateTime.now());
            errandTaskMapper.updateById(task);

            // 发送24小时自动确认延时消息
            long deliverTimestamp = task.getDeliverTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            runAfterCommit(() -> errandDelayMessageService.sendAutoConfirmMessage(taskId, deliverTimestamp));

            log.info("跑腿任务送达: taskId={}, 已加入24小时自动确认队列", taskId);

            // 通知发布者确认
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务已送达",
                    "您的任务 [" + task.getTitle() + "] 跑腿员已送达，请确认收货。24小时后将自动确认。",
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            // 同步到ES
            sendErrandSyncAfterCommit(taskId, ErrandSyncMessage.updateMessage(taskId), "送达");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("送达过程中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmErrand(Long taskId) {
        String lockKey = "errand:lock:lifecycle:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            ErrandTask task = errandTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在");
            }

            // 权限验证已由 @PreAuthorize 在Controller层完成
            if (!ErrandStatus.PENDING_CONFIRM.getCode().equals(task.getTaskStatus())) {
                throw new BusinessException("当前任务状态不可确认完成");
            }

            if (task.getAcceptorId() == null) {
                throw new BusinessException("任务接单人不存在，无法结算");
            }

            // 结算佣金给接单人
            UserInfo acceptor = userInfoMapper.selectById(task.getAcceptorId());
            if (acceptor != null) {
                acceptor.setMoney(acceptor.getMoney().add(task.getReward()));
                userInfoMapper.updateById(acceptor);
            }

            // 更新状态为已完成
            task.setTaskStatus(ErrandStatus.COMPLETED.getCode());
            task.setConfirmTime(LocalDateTime.now());
            errandTaskMapper.updateById(task);

            log.info("跑腿任务确认完成: taskId={}, 佣金已结算 {}", taskId, task.getReward());

            // 通知接单人
            noticeService.sendNotice(
                    task.getAcceptorId(),
                    "佣金到账",
                    "跑腿任务 [" + task.getTitle() + "] 已确认完成，佣金 ￥" + task.getReward() + " 已到账。",
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            // 同步到ES
            sendErrandSyncAfterCommit(taskId, ErrandSyncMessage.updateMessage(taskId), "确认完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("确认过程中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelErrand(Long taskId, String reason) {
        String lockKey = "errand:lock:lifecycle:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BusinessException("系统繁忙，请稍后重试");
            }

            ErrandTask task = errandTaskMapper.selectById(taskId);
            if (task == null) {
                throw new BusinessException("任务不存在");
            }

            // 权限验证已由 @PreAuthorize 在Controller层完成
            Integer status = task.getTaskStatus();
            boolean canCancel = ErrandStatus.PENDING.getCode().equals(status)
                    || ErrandStatus.IN_PROGRESS.getCode().equals(status);
            if (!canCancel) {
                throw new BusinessException("当前任务状态不可取消");
            }

            // 退还佣金给发布者
            UserInfo publisher = userInfoMapper.selectById(task.getPublisherId());
            if (publisher != null) {
                publisher.setMoney(publisher.getMoney().add(task.getReward()));
                userInfoMapper.updateById(publisher);
            }

            String normalizedReason = reason == null ? "" : reason.trim();
            if (normalizedReason.isEmpty()) {
                normalizedReason = "任务取消";
            }

            // 更新状态为已取消
            task.setTaskStatus(ErrandStatus.CANCELLED.getCode());
            task.setCancelTime(LocalDateTime.now());
            task.setCancelReason(normalizedReason);
            errandTaskMapper.updateById(task);

            log.info("跑腿任务取消: taskId={}, reason={}", taskId, normalizedReason);

            // 发送通知给双方
            if (task.getAcceptorId() != null) {
                noticeService.sendNotice(
                        task.getAcceptorId(),
                        "任务已取消",
                        "跑腿任务 [" + task.getTitle() + "] 已取消。",
                        NoticeType.TRADE.getCode(),
                        taskId
                );
            }
            noticeService.sendNotice(
                    task.getPublisherId(),
                    "任务已取消",
                    "跑腿任务 [" + task.getTitle() + "] 已取消，佣金已退还。",
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            // 同步到ES（删除）
            sendErrandSyncAfterCommit(taskId, ErrandSyncMessage.deleteMessage(taskId), "取消");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("取消过程中断");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public Page<ErrandVO> getMyPublishedErrands(Long userId, PageQuery query) {
        Page<ErrandTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ErrandTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandTask::getPublisherId, userId).orderByDesc(ErrandTask::getCreateTime);

        Page<ErrandTask> taskPage = errandTaskMapper.selectPage(page, wrapper);
        Page<ErrandVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(convertToVOList(taskPage.getRecords()));
        return voPage;
    }

    @Override
    public Page<ErrandVO> getMyAcceptedErrands(Long userId, PageQuery query) {
        Page<ErrandTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ErrandTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandTask::getAcceptorId, userId).orderByDesc(ErrandTask::getCreateTime);

        Page<ErrandTask> taskPage = errandTaskMapper.selectPage(page, wrapper);
        Page<ErrandVO> voPage = new Page<>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        voPage.setRecords(convertToVOList(taskPage.getRecords()));
        return voPage;
    }

    @Override
    public void uploadLocation(Long userId, LocationUploadDTO dto) {
        log.debug("位置上报已停用: userId={}, taskId={}", userId, dto.getTaskId());
    }

    @Override
    public String getLocation(Long taskId) {
        return null;
    }

    private boolean isReviewPassed(Integer reviewStatus) {
        return ReviewStatus.AI_PASSED.getCode().equals(reviewStatus)
                || ReviewStatus.MANUAL_PASSED.getCode().equals(reviewStatus);
    }

    private void sendErrandSyncAfterCommit(Long taskId, ErrandSyncMessage message, String actionName) {
        runAfterCommit(() -> {
            try {
                rocketMQTemplate.convertAndSend(ERRAND_SYNC_TOPIC, message);
                log.info("发送跑腿任务{}同步消息成功: taskId={}, type={}", actionName, taskId, message.getType());
            } catch (Exception e) {
                log.error("发送跑腿任务{}同步消息失败: taskId={}, type={}", actionName, taskId, message.getType(), e);
            }
        });
    }

    // 统一在事务提交后触发异步副作用，避免主事务回滚时消息已提前发出。
    private void runAfterCommit(Runnable task) {
        Runnable safeTask = () -> {
            try {
                task.run();
            } catch (Exception e) {
                log.error("事务提交后执行跑腿异步副作用失败", e);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeTask.run();
                }
            });
            return;
        }
        safeTask.run();
    }

    /**
     * 实体转VO
     */
    private ErrandVO convertToVO(ErrandTask task) {
        return convertToVOList(List.of(task)).get(0);
    }

    /**
     * 批量实体转VO（解决N+1问题）
     */
    private List<ErrandVO> convertToVOList(List<ErrandTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集所有相关的用户ID
        Set<Long> userIds = new java.util.HashSet<>();
        for (ErrandTask task : tasks) {
            userIds.add(task.getPublisherId());
            if (task.getAcceptorId() != null) {
                userIds.add(task.getAcceptorId());
            }
        }

        // 2. 批量查询用户信息
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));

        // 3. 转换并注入信息
        return tasks.stream().map(task -> {
            ErrandVO vo = BeanUtil.copyProperties(task, ErrandVO.class);

            // 状态描述
            ErrandStatus status = ErrandStatus.getByCode(task.getTaskStatus());
            if (status != null) {
                vo.setStatusText(status.getDescription());
            }
            ReviewStatus reviewStatus = ReviewStatus.getByCode(task.getReviewStatus());
            if (reviewStatus != null) {
                vo.setReviewStatusText(reviewStatus.getDescription());
            }

            // 发布者
            UserInfo publisher = userMap.get(task.getPublisherId());
            if (publisher != null) {
                vo.setPublisherName(publisher.getNickName());
                vo.setPublisherAvatar(publisher.getImageUrl());
            }

            // 接单者
            if (task.getAcceptorId() != null) {
                UserInfo acceptor = userMap.get(task.getAcceptorId());
                if (acceptor != null) {
                    vo.setAcceptorName(acceptor.getNickName());
                    vo.setAcceptorAvatar(acceptor.getImageUrl());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }
}

