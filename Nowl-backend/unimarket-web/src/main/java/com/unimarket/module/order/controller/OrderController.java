package com.unimarket.module.order.controller;

import com.unimarket.common.result.PageResult;
import com.unimarket.common.result.Result;
import com.unimarket.module.dispute.dto.OrderDisputeApplyDTO;
import com.unimarket.module.dispute.service.OrderDisputeService;

import com.unimarket.module.order.dto.OrderCreateDTO;
import com.unimarket.module.order.dto.OrderQueryDTO;
import com.unimarket.module.order.dto.RefundApplyDTO;
import com.unimarket.module.order.dto.RefundProcessDTO;
import com.unimarket.module.order.service.OrderService;
import com.unimarket.module.order.vo.OrderVO;
import com.unimarket.security.UserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单Controller
 */
@Slf4j
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderDisputeService orderDisputeService;

    /**
     * 创建订单
     * 权限：仅已认证用户可下单
     */
    @PostMapping
    @PreAuthorize("@bizAuth.canCreateOrder(authentication.principal.userId)")
    public Result<Void> create(@Valid @RequestBody OrderCreateDTO dto) {
        Long userId = UserContextHolder.getUserId();
        orderService.create(userId, dto);
        return Result.success();
    }

    /**
     * 查询订单详情
     * 权限：订单参与者（买家或卖家）可查看
     */
    @GetMapping("/{id}")
    @PreAuthorize("@bizAuth.isOrderParticipant(#id, authentication.principal.userId)")
    public Result<OrderVO> detail(@PathVariable Long id) {
        OrderVO detail = orderService.getDetail(id);
        return Result.success(detail);
    }

    /**
     * 查询我的订单
     */
    @GetMapping("/my")
    public Result<PageResult<OrderVO>> getMyOrders(@Valid OrderQueryDTO dto) {
        Long userId = UserContextHolder.getUserId();
        PageResult<OrderVO> result = orderService.getMyOrders(userId, dto);
        return Result.success(result);
    }

    /**
     * 支付订单
     * 权限：买家且订单待支付
     */
    @PutMapping("/{id}/pay")
    @PreAuthorize("@bizAuth.canPayOrder(#id, authentication.principal.userId)")
    public Result<Void> pay(@PathVariable Long id) {
        orderService.pay(id);
        return Result.success();
    }

    /**
     * 发货
     * 权限：卖家且订单待发货
     */
    @PutMapping("/{id}/deliver")
    @PreAuthorize("@bizAuth.canDeliverOrder(#id, authentication.principal.userId)")
    public Result<Void> deliver(@PathVariable Long id) {
        orderService.deliver(id);
        return Result.success();
    }

    /**
     * 确认收货
     * 权限：买家且订单待收货
     */
    @PutMapping("/{id}/confirm")
    @PreAuthorize("@bizAuth.canConfirmOrder(#id, authentication.principal.userId)")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.success();
    }

    /**
     * 取消订单
     * 权限：买家且订单状态允许取消
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("@bizAuth.canCancelOrder(#id, authentication.principal.userId)")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.success();
    }

    /**
     * 买家申请退款
     */
    @PostMapping("/{id}/refund")
    @PreAuthorize("@bizAuth.canApplyRefundOrder(#id, authentication.principal.userId)")
    public Result<Void> applyRefund(@PathVariable Long id, @Valid @RequestBody RefundApplyDTO dto) {
        Long userId = UserContextHolder.getUserId();
        orderService.applyRefund(id, userId, dto);
        return Result.success();
    }

    /**
     * 卖家处理退款（同意/拒绝）
     */
    @PutMapping("/{id}/refund/process")
    @PreAuthorize("@bizAuth.canProcessRefundOrder(#id, authentication.principal.userId)")
    public Result<Void> processRefund(@PathVariable Long id, @Valid @RequestBody RefundProcessDTO dto) {
        Long userId = UserContextHolder.getUserId();
        orderService.processRefund(id, userId, dto);
        return Result.success();
    }

    /**
     * 发起纠纷申请
     * 权限：订单参与者（买家或卖家）
     */
    @PostMapping("/dispute")
    @PreAuthorize("@bizAuth.canApplyOrderDispute(#dto.orderId, authentication.principal.userId)")
    public Result<Void> applyDispute(@Valid @RequestBody OrderDisputeApplyDTO dto) {
        Long userId = UserContextHolder.getUserId();
        orderDisputeService.applyDispute(userId, dto);
        return Result.success();
    }

}
