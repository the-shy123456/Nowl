package com.unimarket.admin.service;

import com.unimarket.admin.service.impl.domain.AdminDisputeDomainService;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.enums.DisputeStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.CreditScoreService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDisputeDomainServiceTest {

    @Mock
    private DisputeRecordMapper disputeRecordMapper;
    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private GoodsInfoMapper goodsInfoMapper;
    @Mock
    private CreditScoreService creditScoreService;
    @Mock
    private NoticeService noticeService;
    @Mock
    private IamAccessService iamAccessService;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private AdminActionLockSupport actionLockSupport;
    @Mock
    private AdminScopeSupport scopeSupport;
    @Mock
    private AdminSchoolInfoSupport schoolInfoSupport;

    @InjectMocks
    private AdminDisputeDomainService disputeDomainService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(actionLockSupport).withLock(anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("handleDispute: 处理完成后通知双方并携带纠纷记录ID作为relatedId")
    void handleDispute_sendNoticeWithRecordId() {
        DisputeRecord record = new DisputeRecord();
        record.setRecordId(10L);
        record.setInitiatorId(100L);
        record.setRelatedId(200L);
        record.setSchoolCode("SC001");
        record.setCampusCode("C001");
        record.setHandleStatus(DisputeStatus.PENDING.getCode());

        when(disputeRecordMapper.selectById(10L)).thenReturn(record);
        when(disputeRecordMapper.updateById(any(DisputeRecord.class))).thenReturn(1);

        disputeDomainService.handleDispute(
                1L,
                10L,
                "同意退款",
                2,
                null,
                null
        );

        verify(iamAccessService).assertCanManageScope(1L, "SC001", "C001");
        verify(noticeService).sendNotice(
                100L,
                "纠纷处理结果",
                "您发起的纠纷已处理，结果：同意退款",
                NoticeType.DISPUTE.getCode(),
                10L
        );
        verify(noticeService).sendNotice(
                200L,
                "纠纷处理结果",
                "涉及您的纠纷已处理，结果：同意退款",
                NoticeType.DISPUTE.getCode(),
                10L
        );
    }

    @Test
    @DisplayName("handleDispute: 已终态且请求一致时按幂等忽略")
    void handleDispute_terminalSameRequest_idempotentIgnore() {
        DisputeRecord record = new DisputeRecord();
        record.setRecordId(10L);
        record.setInitiatorId(100L);
        record.setRelatedId(200L);
        record.setSchoolCode("SC001");
        record.setCampusCode("C001");
        record.setHandleStatus(DisputeStatus.RESOLVED.getCode());
        record.setHandleResult("同意退款");
        when(disputeRecordMapper.selectById(10L)).thenReturn(record);

        disputeDomainService.handleDispute(
                1L,
                10L,
                "同意退款",
                DisputeStatus.RESOLVED.getCode(),
                null,
                null
        );

        verify(disputeRecordMapper, never()).updateById(any(DisputeRecord.class));
        verify(noticeService, never()).sendNotice(anyLong(), anyString(), anyString(), anyInt(), anyLong());
    }

    @Test
    @DisplayName("handleDispute: 已终态且请求冲突时抛出异常")
    void handleDispute_terminalConflict_throw() {
        DisputeRecord record = new DisputeRecord();
        record.setRecordId(10L);
        record.setInitiatorId(100L);
        record.setRelatedId(200L);
        record.setSchoolCode("SC001");
        record.setCampusCode("C001");
        record.setHandleStatus(DisputeStatus.RESOLVED.getCode());
        record.setHandleResult("同意退款");
        when(disputeRecordMapper.selectById(10L)).thenReturn(record);

        assertThrows(BusinessException.class, () -> disputeDomainService.handleDispute(
                1L,
                10L,
                "拒绝退款",
                DisputeStatus.REJECTED.getCode(),
                null,
                null
        ));
    }
}
