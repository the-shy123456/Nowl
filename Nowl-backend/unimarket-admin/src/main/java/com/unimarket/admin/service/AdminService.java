package com.unimarket.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.vo.DisputeVO;
import com.unimarket.common.result.PageQuery;
import com.unimarket.module.errand.vo.ErrandVO;
import com.unimarket.module.goods.vo.GoodsVO;
import com.unimarket.module.order.vo.OrderVO;
import com.unimarket.module.user.vo.UserInfoVO;
import com.unimarket.admin.vo.DashboardStatsVO;

import java.math.BigDecimal;

/**
 * 管理后台Service接口
 */
public interface AdminService {

    /**
     * 获取仪表盘统计数据
     */
    DashboardStatsVO getDashboardStats(Long operatorId);

    /**
     * 审核商品
     */
    void auditGoods(Long operatorId, Long goodsId, Integer status, String reason);

    /**
     * 获取待审核商品列表
     */
    Page<GoodsVO> getPendingAuditGoods(Long operatorId,
                                       PageQuery query,
                                       String schoolCode,
                                       String campusCode,
                                       Integer reviewStatus);

    /**
     * 获取全部商品列表（支持关键词搜索）
     */
    Page<GoodsVO> getAllGoods(Long operatorId,
                              PageQuery query,
                              String keyword,
                              String schoolCode,
                              String campusCode,
                              Integer tradeStatus,
                              Integer reviewStatus);

    /**
     * 强制下架商品
     */
    void forceOfflineGoods(Long operatorId, Long goodsId, String reason);

    /**
     * 更新用户状态（封禁/解封）
     */
    void updateUserStatus(Long operatorId, Long userId, Integer status);

    /**
     * 获取用户列表（支持关键词搜索）
     */
    Page<UserInfoVO> getUserList(Long operatorId,
                                 PageQuery query,
                                 String keyword,
                                 String schoolCode,
                                 String campusCode,
                                 Integer accountStatus,
                                 Integer authStatus);

    /**
     * 获取待认证用户列表
     */
    Page<UserInfoVO> getPendingAuthUsers(Long operatorId, PageQuery query, String schoolCode, String campusCode);

    /**
     * 审核用户认证
     */
    void auditUserAuth(Long operatorId, Long userId, Integer status, String reason);

    Page<UserInfoVO> getPendingRunnerUsers(Long operatorId, PageQuery query, String schoolCode, String campusCode);

    void auditRunner(Long operatorId, Long userId, Integer status, String reason);

    /**
     * 获取订单列表（支持关键词搜索）
     */
    Page<OrderVO> getOrderList(Long operatorId,
                               PageQuery query,
                               String keyword,
                               String schoolCode,
                               String campusCode,
                               Integer orderStatus);

    /**
     * 获取纠纷列表
     */
    Page<DisputeVO> getDisputeList(Long operatorId,
                                   PageQuery query,
                                   Integer status,
                                   Integer targetType,
                                   String schoolCode,
                                   String campusCode);

    /**
     * 处理纠纷
     */
    void handleDispute(Long operatorId, Long disputeId, String result, Integer handleStatus, Integer deductCreditScore, BigDecimal refundAmount);

    /**
     * 获取跑腿任务列表（管理员，支持搜索和状态筛选）
     */
    Page<ErrandVO> getAdminErrandList(Long operatorId,
                                      PageQuery query,
                                      String keyword,
                                      Integer status,
                                      Integer reviewStatus,
                                      String schoolCode,
                                      String campusCode);

    /**
     * 人工复核跑腿任务
     */
    void auditErrand(Long operatorId, Long taskId, Integer status, String reason);

    /**
     * 获取用户详情（含信用分）
     */
    UserInfoVO getUserDetail(Long operatorId, Long userId);

    /**
     * 调整用户信用分
     */
    void adjustUserCreditScore(Long operatorId, Long userId, int change, String reason);

    /**
     * 广播系统通知给所有活跃用户
     */
    void broadcastNotice(Long operatorId, String title, String content, String schoolCode, String campusCode);
}
