package com.unimarket.module.errand.service.impl;

import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.errand.service.ErrandDelayMessageService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.risk.service.RiskControlService;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.user.mapper.UserInfoMapper;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrandServiceImplTest {

    @Mock
    private ErrandTaskMapper errandTaskMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private SchoolInfoMapper schoolInfoMapper;
    @Mock
    private NoticeService noticeService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private ErrandDelayMessageService errandDelayMessageService;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private RiskControlService riskControlService;
    @Mock
    private RLock lock;

    @InjectMocks
    private ErrandServiceImpl errandService;

    @Test
    @DisplayName("cancelErrand: 审核驳回任务不可重复取消退款")
    void cancelErrand_rejectedTask_throwWithoutRefund() throws Exception {
        ErrandTask task = new ErrandTask();
        task.setTaskId(88L);
        task.setPublisherId(1L);
        task.setTaskStatus(ErrandStatus.PENDING.getCode());
        task.setReviewStatus(ReviewStatus.REJECTED.getCode());

        when(redissonClient.getLock("errand:lock:lifecycle:88")).thenReturn(lock);
        when(lock.tryLock(3, 10, java.util.concurrent.TimeUnit.SECONDS)).thenReturn(true);
        when(errandTaskMapper.selectById(88L)).thenReturn(task);

        assertThrows(BusinessException.class, () -> errandService.cancelErrand(88L, "不想发了"));

        verify(userInfoMapper, never()).selectById(any());
        verify(errandTaskMapper, never()).updateById(any(ErrandTask.class));
        verify(noticeService, never()).sendNotice(any(), any(), any(), any(), any());
        verify(lock, never()).unlock();
    }
}

