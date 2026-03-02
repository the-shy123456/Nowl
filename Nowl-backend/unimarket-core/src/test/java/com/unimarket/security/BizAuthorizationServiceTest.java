package com.unimarket.security;

import com.unimarket.module.errand.service.ErrandPermissionService;
import com.unimarket.module.goods.service.GoodsPermissionService;
import com.unimarket.module.order.service.OrderPermissionService;
import com.unimarket.module.dispute.service.DisputePermissionService;
import com.unimarket.module.risk.service.RiskBehaviorControlService;
import com.unimarket.module.user.service.UserPermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BizAuthorizationServiceTest {

    @Mock
    private UserPermissionService userPermissionService;
    @Mock
    private GoodsPermissionService goodsPermissionService;
    @Mock
    private OrderPermissionService orderPermissionService;
    @Mock
    private ErrandPermissionService errandPermissionService;
    @Mock
    private DisputePermissionService disputePermissionService;
    @Mock
    private RiskBehaviorControlService riskBehaviorControlService;

    @InjectMocks
    private BizAuthorizationService bizAuthorizationService;

    @Test
    @DisplayName("canPublishGoods: 需实名认证且发布行为未被硬阻断")
    void canPublishGoods_requireAuthAndRiskPass() {
        when(userPermissionService.isAuthenticated(1L)).thenReturn(true);
        when(riskBehaviorControlService.isAllowed(1L, null)).thenReturn(true);

        assertTrue(bizAuthorizationService.canPublishGoods(1L));
    }

    @Test
    @DisplayName("canCreateOrder: 命中ALL硬阻断时拒绝")
    void canCreateOrder_globalBlocked_false() {
        when(userPermissionService.isAuthenticated(2L)).thenReturn(true);
        when(riskBehaviorControlService.isAllowed(2L, null)).thenReturn(false);

        assertFalse(bizAuthorizationService.canCreateOrder(2L));
    }

    @Test
    @DisplayName("canMutate: 命中ALL硬阻断时拒绝写操作")
    void canMutate_globalBlocked_false() {
        when(riskBehaviorControlService.isAllowed(12L, null)).thenReturn(false);

        assertFalse(bizAuthorizationService.canMutate(12L));
    }

    @Test
    @DisplayName("canPayOrder: 全局通过后依赖订单状态与参与者判定")
    void canPayOrder_requireGlobalAndOrderPermission() {
        when(riskBehaviorControlService.isAllowed(3L, null)).thenReturn(true);
        when(orderPermissionService.canPay(88L, 3L)).thenReturn(true);

        assertTrue(bizAuthorizationService.canPayOrder(88L, 3L));
        verify(orderPermissionService).canPay(88L, 3L);
    }

    @Test
    @DisplayName("canAcceptErrand: taskId为空直接拒绝")
    void canAcceptErrand_taskIdNull_false() {
        assertFalse(bizAuthorizationService.canAcceptErrand(null, 4L));
    }

    @Test
    @DisplayName("canAcceptErrand: 需跑腿员身份且行为未被硬阻断")
    void canAcceptErrand_requireRunnerAndRiskPass() {
        when(userPermissionService.isAuthenticatedRunner(5L)).thenReturn(true);
        when(riskBehaviorControlService.isAllowed(5L, null)).thenReturn(true);

        assertTrue(bizAuthorizationService.canAcceptErrand(99L, 5L));
    }

    @Test
    @DisplayName("canUploadErrandLocation: 命中ALL硬阻断时拒绝")
    void canUploadErrandLocation_globalBlocked_false() {
        when(riskBehaviorControlService.isAllowed(6L, null)).thenReturn(false);

        assertFalse(bizAuthorizationService.canUploadErrandLocation(123L, 6L));
    }

    @Test
    @DisplayName("canWithdrawDispute: 需通过全局行为管控且满足纠纷撤回条件")
    void canWithdrawDispute_requireGlobalAndOwnership() {
        when(riskBehaviorControlService.isAllowed(7L, null)).thenReturn(true);
        when(disputePermissionService.canWithdraw(7L, 501L)).thenReturn(true);

        assertTrue(bizAuthorizationService.canWithdrawDispute(501L, 7L));
    }
}
