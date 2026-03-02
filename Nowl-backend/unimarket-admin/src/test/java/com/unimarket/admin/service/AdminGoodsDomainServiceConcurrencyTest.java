package com.unimarket.admin.service;

import com.unimarket.admin.service.impl.domain.AdminGoodsDomainService;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.enums.TradeStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.user.mapper.UserInfoMapper;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminGoodsDomainServiceConcurrencyTest {

    @Mock
    private GoodsInfoMapper goodsInfoMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
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
    private AdminGoodsDomainService goodsDomainService;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(actionLockSupport).withLock(anyString(), any(Runnable.class));
    }

    @Test
    @DisplayName("auditGoods: 终态同结果请求幂等忽略")
    void auditGoods_terminalSameStatus_idempotentIgnore() {
        GoodsInfo goods = new GoodsInfo();
        goods.setProductId(100L);
        goods.setSellerId(200L);
        goods.setSchoolCode("SC001");
        goods.setCampusCode("CP001");
        goods.setTradeStatus(TradeStatus.ON_SALE.getCode());
        goods.setReviewStatus(ReviewStatus.MANUAL_PASSED.getCode());
        when(goodsInfoMapper.selectById(100L)).thenReturn(goods);

        goodsDomainService.auditGoods(1L, 100L, 1, null);

        verify(goodsInfoMapper, never()).updateById(any(GoodsInfo.class));
        verify(noticeService, never()).sendNotice(any(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("auditGoods: 终态冲突请求抛出异常")
    void auditGoods_terminalConflict_throw() {
        GoodsInfo goods = new GoodsInfo();
        goods.setProductId(100L);
        goods.setSellerId(200L);
        goods.setSchoolCode("SC001");
        goods.setCampusCode("CP001");
        goods.setTradeStatus(TradeStatus.ON_SALE.getCode());
        goods.setReviewStatus(ReviewStatus.REJECTED.getCode());
        when(goodsInfoMapper.selectById(100L)).thenReturn(goods);

        assertThrows(BusinessException.class, () -> goodsDomainService.auditGoods(1L, 100L, 1, "改判"));
        verify(goodsInfoMapper, never()).updateById(any(GoodsInfo.class));
    }
}

