package com.unimarket.admin.service.impl.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.vo.DashboardStatsVO;
import com.unimarket.common.enums.*;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminDashboardDomainService {

    private final GoodsInfoMapper goodsInfoMapper;
    private final UserInfoMapper userInfoMapper;
    private final DisputeRecordMapper disputeRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ErrandTaskMapper errandTaskMapper;
    private final AdminScopeSupport scopeSupport;

    public DashboardStatsVO getDashboardStats(Long operatorId) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        DashboardStatsVO stats = new DashboardStatsVO();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        // 用户统计
        LambdaQueryWrapper<UserInfo> totalUsersWrapper = new LambdaQueryWrapper<>();
        scopeSupport.applyScopeFilter(totalUsersWrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);
        stats.setTotalUsers(userInfoMapper.selectCount(totalUsersWrapper));

        LambdaQueryWrapper<UserInfo> todayUsersWrapper = new LambdaQueryWrapper<UserInfo>()
                .ge(UserInfo::getCreateTime, todayStart);
        scopeSupport.applyScopeFilter(todayUsersWrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);
        stats.setTodayNewUsers(userInfoMapper.selectCount(todayUsersWrapper));

        LambdaQueryWrapper<UserInfo> verifiedUsersWrapper = new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getAuthStatus, AuthStatus.APPROVED.getCode());
        scopeSupport.applyScopeFilter(verifiedUsersWrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);
        stats.setVerifiedUsers(userInfoMapper.selectCount(verifiedUsersWrapper));

        LambdaQueryWrapper<UserInfo> pendingAuthWrapper = new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getAuthStatus, AuthStatus.PENDING.getCode());
        scopeSupport.applyScopeFilter(pendingAuthWrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);
        stats.setPendingAuth(userInfoMapper.selectCount(pendingAuthWrapper));

        LambdaQueryWrapper<UserInfo> pendingRunnerWrapper = new LambdaQueryWrapper<UserInfo>()
                .eq(UserInfo::getRunnableStatus, RunnableStatus.PENDING.getCode());
        scopeSupport.applyScopeFilter(pendingRunnerWrapper, scopes, UserInfo::getSchoolCode, UserInfo::getCampusCode);
        stats.setPendingRunners(userInfoMapper.selectCount(pendingRunnerWrapper));

        // 商品统计
        LambdaQueryWrapper<GoodsInfo> totalGoodsWrapper = new LambdaQueryWrapper<>();
        scopeSupport.applyScopeFilter(totalGoodsWrapper, scopes, GoodsInfo::getSchoolCode, GoodsInfo::getCampusCode);
        stats.setTotalGoods(goodsInfoMapper.selectCount(totalGoodsWrapper));

        LambdaQueryWrapper<GoodsInfo> onSaleGoodsWrapper = new LambdaQueryWrapper<GoodsInfo>()
                .eq(GoodsInfo::getTradeStatus, TradeStatus.ON_SALE.getCode());
        scopeSupport.applyScopeFilter(onSaleGoodsWrapper, scopes, GoodsInfo::getSchoolCode, GoodsInfo::getCampusCode);
        stats.setOnSaleGoods(goodsInfoMapper.selectCount(onSaleGoodsWrapper));

        LambdaQueryWrapper<GoodsInfo> todayGoodsWrapper = new LambdaQueryWrapper<GoodsInfo>()
                .ge(GoodsInfo::getCreateTime, todayStart);
        scopeSupport.applyScopeFilter(todayGoodsWrapper, scopes, GoodsInfo::getSchoolCode, GoodsInfo::getCampusCode);
        stats.setTodayNewGoods(goodsInfoMapper.selectCount(todayGoodsWrapper));

        LambdaQueryWrapper<GoodsInfo> pendingGoodsWrapper = new LambdaQueryWrapper<GoodsInfo>()
                .in(GoodsInfo::getReviewStatus, ReviewStatus.WAIT_REVIEW.getCode(), ReviewStatus.WAIT_MANUAL.getCode());
        scopeSupport.applyScopeFilter(pendingGoodsWrapper, scopes, GoodsInfo::getSchoolCode, GoodsInfo::getCampusCode);
        stats.setPendingGoods(goodsInfoMapper.selectCount(pendingGoodsWrapper));

        // 订单统计
        LambdaQueryWrapper<OrderInfo> totalOrdersWrapper = new LambdaQueryWrapper<>();
        scopeSupport.applyScopeFilter(totalOrdersWrapper, scopes, OrderInfo::getSchoolCode, OrderInfo::getCampusCode);
        stats.setTotalOrders(orderInfoMapper.selectCount(totalOrdersWrapper));

        LambdaQueryWrapper<OrderInfo> todayOrdersWrapper = new LambdaQueryWrapper<OrderInfo>()
                .ge(OrderInfo::getCreateTime, todayStart);
        scopeSupport.applyScopeFilter(todayOrdersWrapper, scopes, OrderInfo::getSchoolCode, OrderInfo::getCampusCode);
        stats.setTodayOrders(orderInfoMapper.selectCount(todayOrdersWrapper));

        LambdaQueryWrapper<OrderInfo> completedOrdersWrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderStatus, OrderStatus.COMPLETED.getCode());
        scopeSupport.applyScopeFilter(completedOrdersWrapper, scopes, OrderInfo::getSchoolCode, OrderInfo::getCampusCode);
        stats.setCompletedOrders(orderInfoMapper.selectCount(completedOrdersWrapper));

        // 交易金额统计
        BigDecimal totalAmount = sumOrderAmount(scopes, OrderStatus.COMPLETED.getCode(), null);
        stats.setTotalAmount(totalAmount.doubleValue());

        BigDecimal todayAmount = sumOrderAmount(scopes, OrderStatus.COMPLETED.getCode(), todayStart);
        stats.setTodayAmount(todayAmount.doubleValue());

        // 纠纷统计：待处理/处理中均需要管理员介入
        LambdaQueryWrapper<DisputeRecord> pendingDisputeWrapper = new LambdaQueryWrapper<DisputeRecord>()
                .in(DisputeRecord::getHandleStatus, DisputeStatus.PENDING.getCode(), DisputeStatus.PROCESSING.getCode());
        scopeSupport.applyScopeFilter(pendingDisputeWrapper, scopes, DisputeRecord::getSchoolCode, DisputeRecord::getCampusCode);
        stats.setPendingDisputes(disputeRecordMapper.selectCount(pendingDisputeWrapper));

        // 跑腿统计
        LambdaQueryWrapper<ErrandTask> totalErrandsWrapper = new LambdaQueryWrapper<>();
        scopeSupport.applyScopeFilter(totalErrandsWrapper, scopes, ErrandTask::getSchoolCode, ErrandTask::getCampusCode);
        stats.setTotalErrands(errandTaskMapper.selectCount(totalErrandsWrapper));

        LambdaQueryWrapper<ErrandTask> activeErrandsWrapper = new LambdaQueryWrapper<ErrandTask>()
                .in(ErrandTask::getTaskStatus,
                        ErrandStatus.PENDING.getCode(),
                        ErrandStatus.IN_PROGRESS.getCode(),
                        ErrandStatus.PENDING_CONFIRM.getCode());
        scopeSupport.applyScopeFilter(activeErrandsWrapper, scopes, ErrandTask::getSchoolCode, ErrandTask::getCampusCode);
        stats.setActiveErrands(errandTaskMapper.selectCount(activeErrandsWrapper));

        return stats;
    }

    private BigDecimal sumOrderAmount(List<IamAdminScopeBinding> scopes, Integer orderStatus, LocalDateTime startTime) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .eq(OrderInfo::getOrderStatus, orderStatus);
        if (startTime != null) {
            wrapper.ge(OrderInfo::getCreateTime, startTime);
        }
        scopeSupport.applyScopeFilter(wrapper, scopes, OrderInfo::getSchoolCode, OrderInfo::getCampusCode);

        return orderInfoMapper.selectList(wrapper).stream()
                .map(OrderInfo::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
