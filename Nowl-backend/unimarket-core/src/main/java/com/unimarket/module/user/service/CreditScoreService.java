package com.unimarket.module.user.service;

/**
 * 信用分服务接口
 */
public interface CreditScoreService {

    /**
     * 信用分上限
     */
    int MAX_CREDIT_SCORE = 150;

    /**
     * 信用分下限
     */
    int MIN_CREDIT_SCORE = 0;

    /**
     * 调整用户信用分
     * @param userId 用户ID
     * @param change 变化值（正数加分，负数扣分）
     * @param reason 原因
     * @return 调整后的信用分
     */
    int adjustCreditScore(Long userId, int change, String reason);

    /**
     * 评价产生的信用分变化
     */
    int adjustByReview(Long userId, int rating);

    /**
     * 纠纷判定有责扣分
     */
    int adjustByDisputeGuilty(Long userId);

    /**
     * 实名认证通过加分
     */
    int adjustByAuthPassed(Long userId);

    /**
     * 买家取消已付款订单扣分
     */
    int adjustByOrderCancel(Long userId);

    /**
     * 买家极速退款扣分
     */
    int adjustByFastRefund(Long userId);

    /**
     * 获取用户信用分
     */
    int getCreditScore(Long userId);
}
