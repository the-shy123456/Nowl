package com.unimarket.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.enums.DisputeStatus;
import com.unimarket.common.enums.DisputeTargetType;
import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.RefundStatus;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单权限校验服务
 * 用于 @PreAuthorize 注解中的权限判断
 *
 * 使用方式：@PreAuthorize("@orderPermission.isBuyer(#id)")
 */
@Slf4j
@Service("orderPermission")
@RequiredArgsConstructor
public class OrderPermissionService {

    private final OrderInfoMapper orderInfoMapper;
    private final DisputeRecordMapper disputeRecordMapper;

    /**
     * 判断当前用户是否为订单买家
     */
    public boolean isBuyer(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在: orderId={}", orderId);
            return false;
        }
        return order.getBuyerId().equals(userId);
    }

    /**
     * 判断当前用户是否为订单卖家
     */
    public boolean isSeller(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在: orderId={}", orderId);
            return false;
        }
        return order.getSellerId().equals(userId);
    }

    /**
     * 判断当前用户是否为订单参与者（买家或卖家）
     */
    public boolean isParticipant(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            log.warn("订单不存在: orderId={}", orderId);
            return false;
        }
        return order.getBuyerId().equals(userId) || order.getSellerId().equals(userId);
    }

    /**
     * 判断当前用户是否可以支付订单（买家且订单待支付）
     */
    public boolean canPay(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        return order.getBuyerId().equals(userId)
                && OrderStatus.PENDING_PAYMENT.getCode().equals(order.getOrderStatus());
    }

    /**
     * 判断当前用户是否可以发货（卖家且订单待发货）
     */
    public boolean canDeliver(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        return order.getSellerId().equals(userId)
                && OrderStatus.PENDING_DELIVERY.getCode().equals(order.getOrderStatus());
    }

    /**
     * 判断当前用户是否可以确认收货（买家且订单待收货）
     */
    public boolean canConfirm(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (!order.getBuyerId().equals(userId)) {
            return false;
        }
        if (!OrderStatus.PENDING_RECEIVE.getCode().equals(order.getOrderStatus())) {
            return false;
        }
        if (RefundStatus.PENDING.getCode().equals(order.getRefundStatus())) {
            return false;
        }
        // 存在进行中纠纷时禁止确认收货，避免资金结算与仲裁冲突
        return !hasActiveOrderDispute(orderId);
    }

    /**
     * 判断当前用户是否可以取消订单
     * - 仅待支付状态支持取消
     */
    public boolean canCancel(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }

        return order.getBuyerId().equals(userId)
                && OrderStatus.PENDING_PAYMENT.getCode().equals(order.getOrderStatus());
    }

    /**
     * 买家是否可申请退款
     * - 待发货、待收货状态可申请
     * - 当前未存在待处理退款
     * - 存在进行中纠纷时禁止申请退款，避免仲裁与退款流程冲突
     */
    public boolean canApplyRefund(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }

        if (!order.getBuyerId().equals(userId)) {
            return false;
        }

        Integer status = order.getOrderStatus();
        boolean orderStatusAllow = OrderStatus.PENDING_DELIVERY.getCode().equals(status)
                || OrderStatus.PENDING_RECEIVE.getCode().equals(status);
        boolean noPendingRefund = !RefundStatus.PENDING.getCode().equals(order.getRefundStatus());

        return orderStatusAllow && noPendingRefund && !hasActiveOrderDispute(orderId);
    }

    /**
     * 卖家是否可处理退款
     */
    public boolean canProcessRefund(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }

        return order.getSellerId().equals(userId)
                && RefundStatus.PENDING.getCode().equals(order.getRefundStatus());
    }

    /**
     * 订单参与者是否可发起纠纷
     * - 必须是买家/卖家
     * - 仅待确认收货订单可发起（资金仍在平台托管）
     * - 退款处理中不可发起（避免纠纷与退款并行）
     * - 同一订单存在进行中纠纷时禁止重复发起
     */
    public boolean canApplyDispute(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            return false;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        boolean isParticipant = userId.equals(order.getBuyerId()) || userId.equals(order.getSellerId());
        if (!isParticipant) {
            return false;
        }
        if (!OrderStatus.PENDING_RECEIVE.getCode().equals(order.getOrderStatus())) {
            return false;
        }
        if (RefundStatus.PENDING.getCode().equals(order.getRefundStatus())) {
            return false;
        }
        return !hasActiveOrderDispute(orderId);
    }

    private boolean hasActiveOrderDispute(Long orderId) {
        LambdaQueryWrapper<DisputeRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DisputeRecord::getContentId, orderId)
                .eq(DisputeRecord::getTargetType, DisputeTargetType.ORDER.getCode())
                .in(DisputeRecord::getHandleStatus, DisputeStatus.PENDING.getCode(), DisputeStatus.PROCESSING.getCode());
        return disputeRecordMapper.selectCount(wrapper) > 0;
    }
}
