package com.unimarket.module.user.service.impl;

import com.unimarket.common.enums.ReviewRating;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.CreditScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 信用分服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditScoreServiceImpl implements CreditScoreService {

    private final UserInfoMapper userInfoMapper;

    // 信用分变化常量
    private static final int DISPUTE_GUILTY_PENALTY = -10;      // 纠纷判定有责
    private static final int AUTH_PASSED_BONUS = 5;             // 实名认证通过
    private static final int ORDER_CANCEL_PENALTY = -2;         // 取消已付款订单
    private static final int FAST_REFUND_PENALTY = -1;          // 极速退款扣分

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustCreditScore(Long userId, int change, String reason) {
        // 使用原子更新防止并发冲突
        int result = userInfoMapper.updateCreditScore(userId, change, MIN_CREDIT_SCORE, MAX_CREDIT_SCORE);
        
        if (result <= 0) {
            log.warn("调整信用分失败或用户不存在: userId={}", userId);
            return 0;
        }

        // 获取更新后的分数（非实时强一致需求，仅用于日志和返回）
        int newScore = getCreditScore(userId);
        log.info("用户{}信用分原子调整，变化: {}，原因: {}，当前约: {}", userId, change, reason, newScore);

        return newScore;
    }

    @Override
    public int adjustByReview(Long userId, int rating) {
        int change = ReviewRating.getCreditChangeByCode(rating);
        if (change == 0) {
            return getCreditScore(userId);
        }
        String reason = rating >= 4 ? "收到好评" : (rating <= 2 ? "收到差评" : "收到评价");
        return adjustCreditScore(userId, change, reason);
    }

    @Override
    public int adjustByDisputeGuilty(Long userId) {
        return adjustCreditScore(userId, DISPUTE_GUILTY_PENALTY, "纠纷判定有责");
    }

    @Override
    public int adjustByAuthPassed(Long userId) {
        return adjustCreditScore(userId, AUTH_PASSED_BONUS, "实名认证通过");
    }

    @Override
    public int adjustByOrderCancel(Long userId) {
        return adjustCreditScore(userId, ORDER_CANCEL_PENALTY, "取消已付款订单");
    }

    @Override
    public int adjustByFastRefund(Long userId) {
        return adjustCreditScore(userId, FAST_REFUND_PENALTY, "极速退款");
    }

    @Override
    public int getCreditScore(Long userId) {
        UserInfo user = userInfoMapper.selectById(userId);
        if (user == null || user.getCreditScore() == null) {
            return 100; // 默认100分
        }
        return user.getCreditScore();
    }
}
