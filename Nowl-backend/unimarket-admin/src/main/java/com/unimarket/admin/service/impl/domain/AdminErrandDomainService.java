package com.unimarket.admin.service.impl.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.mq.ErrandSyncMessage;
import com.unimarket.common.result.PageQuery;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.errand.vo.ErrandVO;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminErrandDomainService {

    private static final String ERRAND_SYNC_TOPIC = "errand-sync-topic";

    private final ErrandTaskMapper errandTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final NoticeService noticeService;
    private final IamAccessService iamAccessService;
    private final RocketMQTemplate rocketMQTemplate;
    private final AdminActionLockSupport actionLockSupport;
    private final AdminScopeSupport scopeSupport;
    private final AdminSchoolInfoSupport schoolInfoSupport;

    @Transactional(rollbackFor = Exception.class)
    public void auditErrand(Long operatorId, Long taskId, Integer status, String reason) {
        String lockKey = "admin:audit:errand:" + taskId;
        actionLockSupport.withLock(lockKey, () -> doAuditErrand(operatorId, taskId, status, reason));
    }

    private void doAuditErrand(Long operatorId, Long taskId, Integer status, String reason) {
        ErrandTask task = errandTaskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("跑腿任务不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, task.getSchoolCode(), task.getCampusCode());

        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("审核状态仅支持 1-通过 或 2-驳回");
        }
        Integer currentReviewStatus = task.getReviewStatus();
        Integer targetReviewStatus = status == 1
                ? ReviewStatus.MANUAL_PASSED.getCode()
                : ReviewStatus.REJECTED.getCode();
        if (targetReviewStatus.equals(currentReviewStatus)) {
            log.info("跑腿审核重复请求已忽略: taskId={}, reviewStatus={}", taskId, currentReviewStatus);
            return;
        }
        if (ReviewStatus.MANUAL_PASSED.getCode().equals(currentReviewStatus)
                || ReviewStatus.REJECTED.getCode().equals(currentReviewStatus)) {
            throw new BusinessException("任务已被其他管理员处理，请刷新后重试");
        }
        if (!ReviewStatus.WAIT_MANUAL.getCode().equals(task.getReviewStatus())
                && !ReviewStatus.WAIT_REVIEW.getCode().equals(task.getReviewStatus())) {
            throw new BusinessException("当前任务状态不可复核");
        }

        if (status == 1) {
            task.setReviewStatus(ReviewStatus.MANUAL_PASSED.getCode());
            task.setAuditReason(null);
            errandTaskMapper.updateById(task);

            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务审核通过",
                    "您的跑腿任务【" + task.getTitle() + "】已通过人工复核并恢复可见。",
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            if (ErrandStatus.CANCELLED.getCode().equals(task.getTaskStatus())) {
                rocketMQTemplate.convertAndSend(ERRAND_SYNC_TOPIC, ErrandSyncMessage.deleteMessage(taskId));
            } else {
                rocketMQTemplate.convertAndSend(ERRAND_SYNC_TOPIC, ErrandSyncMessage.updateMessage(taskId));
            }
        } else {
            String rejectReason = StrUtil.isBlank(reason) ? "任务内容未通过平台审核规范" : reason;
            task.setReviewStatus(ReviewStatus.REJECTED.getCode());
            task.setAuditReason(rejectReason);
            errandTaskMapper.updateById(task);

            noticeService.sendNotice(
                    task.getPublisherId(),
                    "跑腿任务审核未通过",
                    "您的跑腿任务【" + task.getTitle() + "】未通过人工复核。原因：" + rejectReason,
                    NoticeType.TRADE.getCode(),
                    taskId
            );

            rocketMQTemplate.convertAndSend(ERRAND_SYNC_TOPIC, ErrandSyncMessage.deleteMessage(taskId));
        }

        log.info("跑腿任务人工复核完成: taskId={}, status={}, reason={}", taskId, status, reason);
    }

    public Page<ErrandVO> getAdminErrandList(Long operatorId,
                                            PageQuery query,
                                            String keyword,
                                            Integer status,
                                            Integer reviewStatus,
                                            String schoolCode,
                                            String campusCode) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<ErrandTask> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<ErrandTask> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.like(ErrandTask::getTitle, keyword);
        }
        wrapper.eq(status != null, ErrandTask::getTaskStatus, status)
                .eq(reviewStatus != null, ErrandTask::getReviewStatus, reviewStatus)
                .eq(StrUtil.isNotBlank(schoolCode), ErrandTask::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), ErrandTask::getCampusCode, campusCode);
        wrapper.orderByDesc(ErrandTask::getCreateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, ErrandTask::getSchoolCode, ErrandTask::getCampusCode);

        Page<ErrandTask> taskPage = errandTaskMapper.selectPage(page, wrapper);
        List<ErrandTask> records = taskPage.getRecords();

        if (records.isEmpty()) {
            return new Page<ErrandVO>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        }

        Set<Long> userIds = records.stream()
                .flatMap(t -> {
                    if (t.getAcceptorId() != null) {
                        return List.of(t.getPublisherId(), t.getAcceptorId()).stream();
                    }
                    return List.of(t.getPublisherId()).stream();
                })
                .collect(Collectors.toSet());
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getUserId, u -> u));
        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, ErrandTask::getSchoolCode);

        List<ErrandVO> vos = records.stream()
                .map(task -> {
                    ErrandVO vo = BeanUtil.copyProperties(task, ErrandVO.class);
                    ReviewStatus review = ReviewStatus.getByCode(task.getReviewStatus());
                    if (review != null) {
                        vo.setReviewStatusText(review.getDescription());
                    }
                    UserInfo publisher = userMap.get(task.getPublisherId());
                    if (publisher != null) {
                        vo.setPublisherName(publisher.getNickName());
                        vo.setPublisherAvatar(publisher.getImageUrl());
                    }
                    if (task.getAcceptorId() != null) {
                        UserInfo acceptor = userMap.get(task.getAcceptorId());
                        if (acceptor != null) {
                            vo.setAcceptorName(acceptor.getNickName());
                            vo.setAcceptorAvatar(acceptor.getImageUrl());
                        }
                    }
                    schoolInfoSupport.fillSchoolCampusNames(
                            task.getSchoolCode(),
                            task.getCampusCode(),
                            schoolMap,
                            vo::setSchoolName,
                            vo::setCampusName
                    );
                    return vo;
                })
                .toList();

        return new Page<ErrandVO>(taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal()).setRecords(vos);
    }
}
