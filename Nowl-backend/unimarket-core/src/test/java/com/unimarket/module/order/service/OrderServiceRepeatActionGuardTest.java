package com.unimarket.module.order.service;

import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.RefundStatus;
import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.enums.TradeStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.dto.RefundApplyDTO;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.order.service.impl.OrderServiceImpl;
import com.unimarket.module.user.entity.UserInfo;
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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OrderServiceRepeatActionGuardTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private GoodsInfoMapper goodsInfoMapper;
    @Mock
    private UserInfoMapper userInfoMapper;
    @Mock
    private NoticeService noticeService;
    @Mock
    private DisputeRecordMapper disputeRecordMapper;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private OrderDelayMessageService orderDelayMessageService;
    @Mock
    private CreditScoreService creditScoreService;
    @Mock
    private RocketMQTemplate rocketMQTemplate;
    @Mock
    private RLock lifecycleLock;
    @Mock
    private RLock goodsLock;

    @InjectMocks
    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() throws InterruptedException {
        lenient().when(redissonClient.getLock(anyString())).thenAnswer(invocation -> {
            String lockKey = invocation.getArgument(0);
            return lockKey.contains("order:lock:goods:") ? goodsLock : lifecycleLock;
        });
        lenient().when(lifecycleLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(goodsLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(lifecycleLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(goodsLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    @DisplayName("pay: 同一订单重复支付第二次被状态机拦截")
    void pay_repeatSubmit_secondRejected() {
        Long orderId = 500L;
        OrderInfo order = new OrderInfo();
        order.setOrderId(orderId);
        order.setOrderNo("ORD-500");
        order.setBuyerId(100L);
        order.setSellerId(200L);
        order.setProductId(900L);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setOrderStatus(OrderStatus.PENDING_PAYMENT.getCode());

        GoodsInfo goods = new GoodsInfo();
        goods.setProductId(900L);
        goods.setTradeStatus(TradeStatus.ON_SALE.getCode());
        goods.setReviewStatus(ReviewStatus.MANUAL_PASSED.getCode());

        UserInfo buyer = new UserInfo();
        buyer.setUserId(100L);
        buyer.setMoney(new BigDecimal("500.00"));

        when(orderInfoMapper.selectById(orderId)).thenReturn(order);
        when(goodsInfoMapper.selectById(900L)).thenReturn(goods);
        when(userInfoMapper.selectById(100L)).thenReturn(buyer);

        assertDoesNotThrow(() -> orderService.pay(orderId));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.pay(orderId));
        assertTrue(ex.getMessage().contains("订单状态不正确"));

        verify(userInfoMapper, times(1)).updateById(any(UserInfo.class));
        verify(orderInfoMapper, times(1)).updateById(any(OrderInfo.class));
    }

    @Test
    @DisplayName("applyRefund: 同一订单重复申请第二次被状态机拦截")
    void applyRefund_repeatSubmit_secondRejected() {
        Long orderId = 600L;
        Long buyerId = 100L;

        OrderInfo order = new OrderInfo();
        order.setOrderId(orderId);
        order.setOrderNo("ORD-600");
        order.setBuyerId(buyerId);
        order.setSellerId(200L);
        order.setProductId(901L);
        order.setOrderStatus(OrderStatus.PENDING_RECEIVE.getCode());
        order.setTotalAmount(new BigDecimal("88.00"));
        order.setRefundStatus(RefundStatus.NONE.getCode());

        RefundApplyDTO dto = new RefundApplyDTO();
        dto.setReason("收到商品与描述不符");
        dto.setAmount(new BigDecimal("20.00"));

        when(orderInfoMapper.selectById(orderId)).thenReturn(order);
        when(disputeRecordMapper.selectCount(any())).thenReturn(0L);

        assertDoesNotThrow(() -> orderService.applyRefund(orderId, buyerId, dto));

        BusinessException ex = assertThrows(BusinessException.class, () -> orderService.applyRefund(orderId, buyerId, dto));
        assertTrue(ex.getMessage().contains("退款申请已在处理中"));

        verify(orderInfoMapper, times(1)).updateById(any(OrderInfo.class));
    }
}
