package com.unimarket.module.order.service;

import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.RefundStatus;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPermissionServiceTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private DisputeRecordMapper disputeRecordMapper;

    @InjectMocks
    private OrderPermissionService orderPermissionService;

    @Test
    @DisplayName("canApplyRefund: 订单允许且无退款处理中且无进行中纠纷 -> true")
    void canApplyRefund_noActiveDispute_true() {
        OrderInfo order = new OrderInfo();
        order.setBuyerId(1L);
        order.setOrderStatus(OrderStatus.PENDING_RECEIVE.getCode());
        order.setRefundStatus(RefundStatus.NONE.getCode());

        when(orderInfoMapper.selectById(10L)).thenReturn(order);
        when(disputeRecordMapper.selectCount(any())).thenReturn(0L);

        assertTrue(orderPermissionService.canApplyRefund(10L, 1L));
    }

    @Test
    @DisplayName("canApplyRefund: 存在进行中纠纷 -> false")
    void canApplyRefund_hasActiveDispute_false() {
        OrderInfo order = new OrderInfo();
        order.setBuyerId(1L);
        order.setOrderStatus(OrderStatus.PENDING_RECEIVE.getCode());
        order.setRefundStatus(RefundStatus.NONE.getCode());

        when(orderInfoMapper.selectById(11L)).thenReturn(order);
        when(disputeRecordMapper.selectCount(any())).thenReturn(1L);

        assertFalse(orderPermissionService.canApplyRefund(11L, 1L));
    }

    @Test
    @DisplayName("canConfirm: 退款处理中 -> false")
    void canConfirm_refundPending_false() {
        OrderInfo order = new OrderInfo();
        order.setBuyerId(1L);
        order.setOrderStatus(OrderStatus.PENDING_RECEIVE.getCode());
        order.setRefundStatus(RefundStatus.PENDING.getCode());

        when(orderInfoMapper.selectById(12L)).thenReturn(order);

        assertFalse(orderPermissionService.canConfirm(12L, 1L));
    }

    @Test
    @DisplayName("canApplyDispute: 退款处理中 -> false")
    void canApplyDispute_refundPending_false() {
        OrderInfo order = new OrderInfo();
        order.setBuyerId(1L);
        order.setSellerId(2L);
        order.setOrderStatus(OrderStatus.PENDING_RECEIVE.getCode());
        order.setRefundStatus(RefundStatus.PENDING.getCode());

        when(orderInfoMapper.selectById(13L)).thenReturn(order);

        assertFalse(orderPermissionService.canApplyDispute(13L, 1L));
    }
}
