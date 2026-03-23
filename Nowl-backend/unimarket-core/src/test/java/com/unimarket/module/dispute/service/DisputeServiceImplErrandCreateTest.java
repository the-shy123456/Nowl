package com.unimarket.module.dispute.service;

import com.unimarket.common.enums.DisputeTargetType;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.dispute.dto.DisputeCreateDTO;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.dispute.service.impl.DisputeServiceImpl;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.user.mapper.UserInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceImplErrandCreateTest {

    @Mock
    private DisputeRecordMapper disputeRecordMapper;
    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private ErrandTaskMapper errandTaskMapper;
    @Mock
    private GoodsInfoMapper goodsInfoMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private NoticeService noticeService;
    @Mock
    private DisputePermissionService disputePermissionService;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock lock;

    @InjectMocks
    private DisputeServiceImpl disputeService;

    @Test
    @DisplayName("createDispute: 跑腿纠纷仅允许发布者发起")
    void createDispute_errandOnlyPublisherCanInitiate() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        ErrandTask task = new ErrandTask();
        task.setTaskId(10L);
        task.setPublisherId(100L);
        task.setAcceptorId(200L);
        task.setTaskStatus(ErrandStatus.PENDING_CONFIRM.getCode());
        task.setReward(new BigDecimal("20.00"));
        task.setSchoolCode("SC001");
        task.setCampusCode("C001");
        task.setTitle("代取快递");
        when(errandTaskMapper.selectById(10L)).thenReturn(task);

        DisputeCreateDTO dto = new DisputeCreateDTO();
        dto.setContentId(10L);
        dto.setTargetType(DisputeTargetType.ERRAND.getCode());
        dto.setContent("接单人未按约定送达");
        dto.setClaimSellerCreditPenalty(1);
        dto.setClaimRefund(1);
        dto.setClaimRefundAmount(new BigDecimal("20.00"));

        assertThrows(BusinessException.class, () -> disputeService.createDispute(200L, dto));

        verify(disputeRecordMapper, never()).insert(any());
    }
}
