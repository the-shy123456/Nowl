package com.unimarket.admin.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.service.AdminService;
import com.unimarket.admin.service.impl.domain.AdminDashboardDomainService;
import com.unimarket.admin.service.impl.domain.AdminDisputeDomainService;
import com.unimarket.admin.service.impl.domain.AdminErrandDomainService;
import com.unimarket.admin.service.impl.domain.AdminGoodsDomainService;
import com.unimarket.admin.service.impl.domain.AdminNoticeDomainService;
import com.unimarket.admin.service.impl.domain.AdminOrderDomainService;
import com.unimarket.admin.service.impl.domain.AdminUserDomainService;
import com.unimarket.admin.vo.DashboardStatsVO;
import com.unimarket.admin.vo.DisputeVO;
import com.unimarket.common.result.PageQuery;
import com.unimarket.module.errand.vo.ErrandVO;
import com.unimarket.module.goods.vo.GoodsVO;
import com.unimarket.module.order.vo.OrderVO;
import com.unimarket.module.user.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 管理后台服务：对外保持 AdminService 稳定接口，内部按领域拆分实现。
 */
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminDashboardDomainService dashboardDomainService;
    private final AdminGoodsDomainService goodsDomainService;
    private final AdminUserDomainService userDomainService;
    private final AdminOrderDomainService orderDomainService;
    private final AdminDisputeDomainService disputeDomainService;
    private final AdminErrandDomainService errandDomainService;
    private final AdminNoticeDomainService noticeDomainService;

    @Override
    public DashboardStatsVO getDashboardStats(Long operatorId) {
        return dashboardDomainService.getDashboardStats(operatorId);
    }

    @Override
    public void auditGoods(Long operatorId, Long goodsId, Integer status, String reason) {
        goodsDomainService.auditGoods(operatorId, goodsId, status, reason);
    }

    @Override
    public Page<GoodsVO> getPendingAuditGoods(Long operatorId,
                                             PageQuery query,
                                             String schoolCode,
                                             String campusCode,
                                             Integer reviewStatus) {
        return goodsDomainService.getPendingAuditGoods(operatorId, query, schoolCode, campusCode, reviewStatus);
    }

    @Override
    public Page<GoodsVO> getAllGoods(Long operatorId,
                                     PageQuery query,
                                     String keyword,
                                     String schoolCode,
                                     String campusCode,
                                     Integer tradeStatus,
                                     Integer reviewStatus) {
        return goodsDomainService.getAllGoods(operatorId, query, keyword, schoolCode, campusCode, tradeStatus, reviewStatus);
    }

    @Override
    public void forceOfflineGoods(Long operatorId, Long goodsId, String reason) {
        goodsDomainService.forceOfflineGoods(operatorId, goodsId, reason);
    }

    @Override
    public void updateUserStatus(Long operatorId, Long userId, Integer status) {
        userDomainService.updateUserStatus(operatorId, userId, status);
    }

    @Override
    public Page<UserInfoVO> getUserList(Long operatorId,
                                        PageQuery query,
                                        String keyword,
                                        String schoolCode,
                                        String campusCode,
                                        Integer accountStatus,
                                        Integer authStatus) {
        return userDomainService.getUserList(operatorId, query, keyword, schoolCode, campusCode, accountStatus, authStatus);
    }

    @Override
    public Page<UserInfoVO> getPendingAuthUsers(Long operatorId, PageQuery query, String schoolCode, String campusCode) {
        return userDomainService.getPendingAuthUsers(operatorId, query, schoolCode, campusCode);
    }

    @Override
    public void auditUserAuth(Long operatorId, Long userId, Integer status, String reason) {
        userDomainService.auditUserAuth(operatorId, userId, status, reason);
    }

    @Override
    public Page<UserInfoVO> getPendingRunnerUsers(Long operatorId, PageQuery query, String schoolCode, String campusCode) {
        return userDomainService.getPendingRunnerUsers(operatorId, query, schoolCode, campusCode);
    }

    @Override
    public void auditRunner(Long operatorId, Long userId, Integer status, String reason) {
        userDomainService.auditRunner(operatorId, userId, status, reason);
    }

    @Override
    public Page<OrderVO> getOrderList(Long operatorId,
                                      PageQuery query,
                                      String keyword,
                                      String schoolCode,
                                      String campusCode,
                                      Integer orderStatus) {
        return orderDomainService.getOrderList(operatorId, query, keyword, schoolCode, campusCode, orderStatus);
    }

    @Override
    public Page<DisputeVO> getDisputeList(Long operatorId,
                                         PageQuery query,
                                         Integer status,
                                         Integer targetType,
                                         String schoolCode,
                                         String campusCode) {
        return disputeDomainService.getDisputeList(operatorId, query, status, targetType, schoolCode, campusCode);
    }

    @Override
    public void handleDispute(Long operatorId,
                              Long disputeId,
                              String result,
                              Integer handleStatus,
                              Integer deductCreditScore,
                              BigDecimal refundAmount) {
        disputeDomainService.handleDispute(operatorId, disputeId, result, handleStatus, deductCreditScore, refundAmount);
    }

    @Override
    public Page<ErrandVO> getAdminErrandList(Long operatorId,
                                            PageQuery query,
                                            String keyword,
                                            Integer status,
                                            Integer reviewStatus,
                                            String schoolCode,
                                            String campusCode) {
        return errandDomainService.getAdminErrandList(operatorId, query, keyword, status, reviewStatus, schoolCode, campusCode);
    }

    @Override
    public void auditErrand(Long operatorId, Long taskId, Integer status, String reason) {
        errandDomainService.auditErrand(operatorId, taskId, status, reason);
    }

    @Override
    public UserInfoVO getUserDetail(Long operatorId, Long userId) {
        return userDomainService.getUserDetail(operatorId, userId);
    }

    @Override
    public void adjustUserCreditScore(Long operatorId, Long userId, int change, String reason) {
        userDomainService.adjustUserCreditScore(operatorId, userId, change, reason);
    }

    @Override
    public void broadcastNotice(Long operatorId, String title, String content, String schoolCode, String campusCode) {
        noticeDomainService.broadcastNotice(operatorId, title, content, schoolCode, campusCode);
    }
}
