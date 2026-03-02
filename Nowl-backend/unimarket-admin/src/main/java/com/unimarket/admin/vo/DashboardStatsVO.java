package com.unimarket.admin.vo;

import lombok.Data;

/**
 * 管理后台仪表盘统计数据VO
 */
@Data
public class DashboardStatsVO {

    /**
     * 总用户数
     */
    private Long totalUsers;

    /**
     * 今日新增用户
     */
    private Long todayNewUsers;

    /**
     * 已认证用户数
     */
    private Long verifiedUsers;

    /**
     * 总商品数
     */
    private Long totalGoods;

    /**
     * 在售商品数
     */
    private Long onSaleGoods;

    /**
     * 今日新增商品
     */
    private Long todayNewGoods;

    /**
     * 总订单数
     */
    private Long totalOrders;

    /**
     * 今日订单数
     */
    private Long todayOrders;

    /**
     * 已完成订单数
     */
    private Long completedOrders;

    /**
     * 总交易金额
     */
    private Double totalAmount;

    /**
     * 今日交易金额
     */
    private Double todayAmount;

    /**
     * 待审核商品数
     */
    private Long pendingGoods;

    /**
     * 待认证用户数
     */
    private Long pendingAuth;

    /**
     * 待审核跑腿员数
     */
    private Long pendingRunners;

    /**
     * 待处理纠纷数
     */
    private Long pendingDisputes;

    /**
     * 总跑腿任务数
     */
    private Long totalErrands;

    /**
     * 进行中跑腿数
     */
    private Long activeErrands;
}
