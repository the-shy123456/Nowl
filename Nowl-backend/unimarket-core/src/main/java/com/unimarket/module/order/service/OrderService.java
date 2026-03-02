package com.unimarket.module.order.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.module.order.dto.OrderCreateDTO;
import com.unimarket.module.order.dto.OrderQueryDTO;
import com.unimarket.module.order.dto.RefundApplyDTO;
import com.unimarket.module.order.dto.RefundProcessDTO;
import com.unimarket.module.order.vo.OrderVO;

/**
 * 订单Service接口
 */
public interface OrderService {

    /**
     * 创建订单
     */
    void create(Long userId, OrderCreateDTO dto);

    /**
     * 查询订单详情
     */
    OrderVO getDetail(Long orderId);

    /**
     * 查询我的订单
     */
    PageResult<OrderVO> getMyOrders(Long userId, OrderQueryDTO dto);

    /**
     * 支付订单
     */
    void pay(Long orderId);

    /**
     * 发货
     */
    void deliver(Long orderId);

    /**
     * 确认收货
     */
    void confirm(Long orderId);

    /**
     * 取消订单
     */
    void cancel(Long orderId);

    /**
     * 买家申请退款
     */
    void applyRefund(Long orderId, Long userId, RefundApplyDTO dto);

    /**
     * 卖家处理退款
     */
    void processRefund(Long orderId, Long userId, RefundProcessDTO dto);

    /**
     * 超时自动处理退款（24小时）
     */
    void autoProcessRefunds();
}
