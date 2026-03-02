package com.unimarket.security;

import com.unimarket.module.errand.service.ErrandPermissionService;
import com.unimarket.module.goods.service.GoodsPermissionService;
import com.unimarket.module.order.service.OrderPermissionService;
import com.unimarket.module.dispute.service.DisputePermissionService;
import com.unimarket.module.risk.service.RiskBehaviorControlService;
import com.unimarket.module.user.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户侧业务鉴权门面
 */
@Service("bizAuth")
@RequiredArgsConstructor
public class BizAuthorizationService {

    private final UserPermissionService userPermissionService;
    private final GoodsPermissionService goodsPermissionService;
    private final OrderPermissionService orderPermissionService;
    private final ErrandPermissionService errandPermissionService;
    private final DisputePermissionService disputePermissionService;
    private final RiskBehaviorControlService riskBehaviorControlService;

    public boolean canApplyRunner(Long userId) {
        return userPermissionService.isAuthenticated(userId) && isGloballyAllowed(userId);
    }

    /**
     * 通用写操作门禁：命中 ALL=REJECT 时统一阻断
     */
    public boolean canMutate(Long userId) {
        return isGloballyAllowed(userId);
    }

    public boolean canPublishGoods(Long userId) {
        return userPermissionService.isAuthenticated(userId) && isGloballyAllowed(userId);
    }

    public boolean canCreateOrder(Long userId) {
        return userPermissionService.isAuthenticated(userId) && isGloballyAllowed(userId);
    }

    public boolean isOrderParticipant(Long orderId, Long userId) {
        return orderPermissionService.isParticipant(orderId, userId);
    }

    public boolean canPayOrder(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canPay(orderId, userId);
    }

    public boolean canDeliverOrder(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canDeliver(orderId, userId);
    }

    public boolean canConfirmOrder(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canConfirm(orderId, userId);
    }

    public boolean canCancelOrder(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canCancel(orderId, userId);
    }

    public boolean canApplyRefundOrder(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canApplyRefund(orderId, userId);
    }

    public boolean canProcessRefundOrder(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canProcessRefund(orderId, userId);
    }

    public boolean canApplyOrderDispute(Long orderId, Long userId) {
        return isGloballyAllowed(userId) && orderPermissionService.canApplyDispute(orderId, userId);
    }

    public boolean isGoodsOwner(Long goodsId, Long userId) {
        return isGloballyAllowed(userId) && goodsPermissionService.isOwner(goodsId, userId);
    }

    public boolean canPublishErrand(Long userId) {
        return userPermissionService.isAuthenticated(userId) && isGloballyAllowed(userId);
    }

    public boolean canAcceptErrand(Long taskId, Long userId) {
        if (taskId == null) {
            return false;
        }
        return userPermissionService.isAuthenticatedRunner(userId)
                && isGloballyAllowed(userId);
    }

    public boolean canEditErrand(Long taskId, Long userId) {
        return isGloballyAllowed(userId) && errandPermissionService.canEdit(taskId, userId);
    }

    public boolean canDeliverErrand(Long taskId, Long userId) {
        return isGloballyAllowed(userId) && errandPermissionService.canDeliver(taskId, userId);
    }

    public boolean canConfirmErrand(Long taskId, Long userId) {
        return isGloballyAllowed(userId) && errandPermissionService.canConfirm(taskId, userId);
    }

    public boolean canCancelErrand(Long taskId, Long userId) {
        return isGloballyAllowed(userId) && errandPermissionService.canCancel(taskId, userId);
    }

    public boolean canUploadErrandLocation(Long taskId, Long userId) {
        return isGloballyAllowed(userId) && errandPermissionService.isAcceptor(taskId, userId);
    }

    public boolean canViewErrandLocation(Long taskId, Long userId) {
        return errandPermissionService.isParticipant(taskId, userId);
    }

    public boolean canCreateDispute(Long userId) {
        return isGloballyAllowed(userId);
    }

    public boolean canViewDispute(Long recordId, Long userId) {
        return disputePermissionService.canView(userId, recordId);
    }

    public boolean canWithdrawDispute(Long recordId, Long userId) {
        return isGloballyAllowed(userId) && disputePermissionService.canWithdraw(userId, recordId);
    }

    public boolean canAddDisputeEvidence(Long recordId, Long userId) {
        return isGloballyAllowed(userId) && disputePermissionService.isParticipant(userId, recordId);
    }

    private boolean isGloballyAllowed(Long userId) {
        return riskBehaviorControlService.isAllowed(userId, null);
    }
}
