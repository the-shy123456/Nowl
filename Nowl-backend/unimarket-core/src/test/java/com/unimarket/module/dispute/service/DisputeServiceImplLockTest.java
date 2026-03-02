package com.unimarket.module.dispute.service;

import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.dispute.dto.DisputeCreateDTO;
import com.unimarket.module.dispute.dto.DisputeReplyDTO;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.dispute.service.impl.DisputeServiceImpl;
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

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceImplLockTest {

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
    @DisplayName("createDispute: 抢锁失败时立即返回系统繁忙并且不落库")
    void createDispute_lockBusy_throw() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        DisputeCreateDTO dto = new DisputeCreateDTO();
        dto.setContentId(1L);
        dto.setTargetType(0);
        dto.setContent("并发发起纠纷测试");
        dto.setClaimSellerCreditPenalty(0);
        dto.setClaimRefund(0);

        BusinessException ex = assertThrows(BusinessException.class, () -> disputeService.createDispute(100L, dto));
        assertTrue(ex.getMessage().contains("系统繁忙"));
        verify(disputeRecordMapper, never()).insert(any(DisputeRecord.class));
    }

    @Test
    @DisplayName("addEvidence: 抢锁失败时立即返回系统繁忙并且不更新记录")
    void addEvidence_lockBusy_throw() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        DisputeReplyDTO dto = new DisputeReplyDTO();
        dto.setRecordId(200L);
        dto.setAdditionalContent("补充证据");

        BusinessException ex = assertThrows(BusinessException.class, () -> disputeService.addEvidence(100L, dto));
        assertTrue(ex.getMessage().contains("系统繁忙"));
        verify(disputeRecordMapper, never()).updateById(any(DisputeRecord.class));
    }
}
