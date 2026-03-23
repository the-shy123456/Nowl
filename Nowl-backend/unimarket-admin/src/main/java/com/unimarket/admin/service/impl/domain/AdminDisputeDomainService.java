package com.unimarket.admin.service.impl.domain;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.admin.service.impl.support.AdminActionLockSupport;
import com.unimarket.admin.service.impl.support.AdminScopeSupport;
import com.unimarket.admin.service.impl.support.AdminSchoolInfoSupport;
import com.unimarket.admin.vo.DisputeVO;
import com.unimarket.common.enums.DisputeStatus;
import com.unimarket.common.enums.DisputeTargetType;
import com.unimarket.common.enums.ErrandStatus;
import com.unimarket.common.enums.NoticeType;
import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.RefundStatus;
import com.unimarket.common.enums.TradeStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.common.config.RocketMQConfig;
import com.unimarket.common.mq.GoodsSyncMessage;
import com.unimarket.module.errand.entity.ErrandTask;
import com.unimarket.module.errand.mapper.ErrandTaskMapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.common.result.PageQuery;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import com.unimarket.module.iam.service.IamAccessService;
import com.unimarket.module.notice.service.NoticeService;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.user.entity.UserInfo;
import com.unimarket.module.user.mapper.UserInfoMapper;
import com.unimarket.module.user.service.CreditScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDisputeDomainService {

    private final DisputeRecordMapper disputeRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final ErrandTaskMapper errandTaskMapper;
    private final UserInfoMapper userInfoMapper;
    private final GoodsInfoMapper goodsInfoMapper;
    private final CreditScoreService creditScoreService;
    private final NoticeService noticeService;
    private final IamAccessService iamAccessService;
    private final RocketMQTemplate rocketMQTemplate;
    private final AdminActionLockSupport actionLockSupport;
    private final AdminScopeSupport scopeSupport;
    private final AdminSchoolInfoSupport schoolInfoSupport;

    @Transactional(rollbackFor = Exception.class)
    public void handleDispute(Long operatorId,
                              Long disputeId,
                              String result,
                              Integer handleStatus,
                              Integer deductCreditScore,
                              BigDecimal refundAmount) {
        String lockKey = "admin:dispute:handle:" + disputeId;
        actionLockSupport.withLock(lockKey, () -> doHandleDispute(
                operatorId,
                disputeId,
                result,
                handleStatus,
                deductCreditScore,
                refundAmount
        ));
    }

    private void doHandleDispute(Long operatorId,
                                 Long disputeId,
                                 String result,
                                 Integer handleStatus,
                                 Integer deductCreditScore,
                                 BigDecimal refundAmount) {
        DisputeRecord record = disputeRecordMapper.selectById(disputeId);
        if (record == null) {
            throw new BusinessException("纠纷记录不存在");
        }
        iamAccessService.assertCanManageScope(operatorId, record.getSchoolCode(), record.getCampusCode());

        Integer resolvedStatus = DisputeStatus.RESOLVED.getCode();
        Integer rejectedStatus = DisputeStatus.REJECTED.getCode();
        int normalizedHandleStatus = handleStatus == null ? resolvedStatus : handleStatus;
        if (normalizedHandleStatus != resolvedStatus && normalizedHandleStatus != rejectedStatus) {
            throw new BusinessException("处理状态仅支持已解决或已驳回");
        }
        String normalizedResult = StrUtil.trim(result);
        if (StrUtil.isBlank(normalizedResult)) {
            throw new BusinessException("处理结果不能为空");
        }
        boolean processing = DisputeStatus.PENDING.getCode().equals(record.getHandleStatus())
                || DisputeStatus.PROCESSING.getCode().equals(record.getHandleStatus());
        if (!processing) {
            boolean sameStatus = Integer.valueOf(normalizedHandleStatus).equals(record.getHandleStatus());
            String existedResult = StrUtil.trim(record.getHandleResult());
            boolean sameResult = StrUtil.equals(normalizedResult, existedResult)
                    || StrUtil.equals(extractHandleRemark(existedResult), normalizedResult);
            if (sameStatus && sameResult) {
                log.info("纠纷重复处理请求已忽略: disputeId={}, status={}", disputeId, normalizedHandleStatus);
                return;
            }
            throw new BusinessException("纠纷已被其他管理员处理，请刷新后重试");
        }

        boolean resolved = normalizedHandleStatus == resolvedStatus;

        int actualCreditPenalty = normalizeCreditPenalty(record, resolved, deductCreditScore);
        if (actualCreditPenalty > 0) {
            creditScoreService.adjustCreditScore(record.getRelatedId(), -actualCreditPenalty, "纠纷处理扣分");
        }

        BigDecimal actualRefundAmount = BigDecimal.ZERO;

        if (DisputeTargetType.ORDER.getCode().equals(record.getTargetType())) {
            actualRefundAmount = settleOrderAfterDispute(operatorId, record, resolved, refundAmount);
        } else if (DisputeTargetType.ERRAND.getCode().equals(record.getTargetType())) {
            actualRefundAmount = settleErrandAfterDispute(operatorId, record, resolved, refundAmount);
        }

        record.setResolvedRefundAmount(hasPositiveAmount(actualRefundAmount) ? actualRefundAmount : null);
        record.setResolvedCreditPenalty(actualCreditPenalty > 0 ? actualCreditPenalty : null);
        String finalHandleResult = buildFinalHandleResult(normalizedHandleStatus, normalizedResult, actualCreditPenalty, actualRefundAmount);

        record.setHandleResult(finalHandleResult);
        record.setHandleStatus(normalizedHandleStatus);
        record.setHandlerId(operatorId);
        record.setHandleTime(LocalDateTime.now());
        disputeRecordMapper.updateById(record);
        log.info("纠纷处理完成: disputeId={}, status={}, result={}", disputeId, normalizedHandleStatus, finalHandleResult);

        // 发送通知给双方
        noticeService.sendNotice(record.getInitiatorId(), "纠纷处理结果",
                "您发起的纠纷已处理，结果：" + finalHandleResult,
                NoticeType.DISPUTE.getCode(),
                record.getRecordId());
        noticeService.sendNotice(record.getRelatedId(), "纠纷处理结果",
                "涉及您的纠纷已处理，结果：" + finalHandleResult,
                NoticeType.DISPUTE.getCode(),
                record.getRecordId());
    }

    private String buildFinalHandleResult(Integer handleStatus,
                                          String result,
                                          Integer actualCreditPenalty,
                                          BigDecimal actualRefundAmount) {
        if (DisputeStatus.REJECTED.getCode().equals(handleStatus)) {
            return "纠纷已驳回；处理说明：" + result;
        }

        List<String> actionParts = new ArrayList<>();
        if (actualRefundAmount != null && actualRefundAmount.compareTo(BigDecimal.ZERO) > 0) {
            actionParts.add("退款¥" + formatMoney(actualRefundAmount));
        }
        if (actualCreditPenalty != null && actualCreditPenalty > 0) {
            actionParts.add("扣除对方信用分" + actualCreditPenalty + "分");
        }
        String summary = actionParts.isEmpty() ? "纠纷已处理" : String.join("，", actionParts);
        return summary + "；处理说明：" + result;
    }

    private int normalizeCreditPenalty(DisputeRecord record, boolean resolved, Integer deductCreditScore) {
        if (!resolved
                || record.getClaimSellerCreditPenalty() == null
                || record.getClaimSellerCreditPenalty() != 1
                || deductCreditScore == null
                || deductCreditScore <= 0) {
            return 0;
        }
        return deductCreditScore;
    }

    private String formatMoney(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private boolean hasPositiveAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private String extractHandleRemark(String existedResult) {
        if (StrUtil.isBlank(existedResult)) {
            return existedResult;
        }
        String marker = "处理说明：";
        int markerIndex = existedResult.indexOf(marker);
        if (markerIndex < 0) {
            return existedResult;
        }
        return StrUtil.trim(existedResult.substring(markerIndex + marker.length()));
    }

    private BigDecimal settleOrderAfterDispute(Long operatorId,
                                               DisputeRecord record,
                                               boolean resolved,
                                               BigDecimal refundAmount) {
        OrderInfo order = orderInfoMapper.selectById(record.getContentId());
        if (order == null) {
            return BigDecimal.ZERO;
        }

        if (!OrderStatus.PENDING_RECEIVE.getCode().equals(order.getOrderStatus())) {
            // 历史数据可能存在已完成/已取消订单发起纠纷的情况；为避免重复结算，这里仅对待收货订单执行资金结算。
            log.info("订单状态非待收货，跳过纠纷结算: disputeId={}, orderId={}, status={}",
                    record.getRecordId(), order.getOrderId(), order.getOrderStatus());
            return BigDecimal.ZERO;
        }

        BigDecimal actualRefund = BigDecimal.ZERO;
        if (resolved
                && record.getClaimRefund() != null
                && record.getClaimRefund() == 1
                && refundAmount != null
                && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxAmount = order.getTotalAmount();
            if (record.getClaimRefundAmount() != null && record.getClaimRefundAmount().compareTo(maxAmount) < 0) {
                maxAmount = record.getClaimRefundAmount();
            }
            if (refundAmount.compareTo(maxAmount) > 0) {
                throw new BusinessException("退款金额不能超过可裁定金额");
            }
            actualRefund = refundAmount;
        }

        // 退款：返还买家
        if (actualRefund.compareTo(BigDecimal.ZERO) > 0) {
            UserInfo buyer = userInfoMapper.selectById(order.getBuyerId());
            if (buyer != null) {
                buyer.setMoney(buyer.getMoney().add(actualRefund));
                userInfoMapper.updateById(buyer);
            }
            order.setRefundStatus(RefundStatus.APPROVED.getCode());
            order.setRefundAmount(actualRefund);
            order.setRefundProcessTime(LocalDateTime.now());
            order.setRefundProcessorId(operatorId);
            order.setRefundProcessRemark("纠纷裁定退款");
            order.setRefundFastTrack(0);
            order.setRefundDeadline(null);
        }

        // 结算：剩余金额转入卖家
        BigDecimal sellerIncome = order.getTotalAmount().subtract(actualRefund);
        if (sellerIncome.compareTo(BigDecimal.ZERO) > 0) {
            UserInfo seller = userInfoMapper.selectById(order.getSellerId());
            if (seller != null) {
                seller.setMoney(seller.getMoney().add(sellerIncome));
                userInfoMapper.updateById(seller);
            }
        }

        // 订单结束：不属于正常完成路径
        order.setOrderStatus(OrderStatus.ENDED.getCode());
        orderInfoMapper.updateById(order);

        // 全额退款：商品重新上架（资金全额退回，视为交易关闭）
        if (resolved && actualRefund.compareTo(order.getTotalAmount()) == 0) {
            GoodsInfo goods = goodsInfoMapper.selectById(order.getProductId());
            if (goods != null) {
                goods.setTradeStatus(TradeStatus.ON_SALE.getCode());
                goodsInfoMapper.updateById(goods);
                sendGoodsSyncAfterCommit(GoodsSyncMessage.updateMessage(order.getProductId()));
            }
        }

        log.info("订单纠纷结算完成: disputeId={}, orderId={}, refund={}, sellerIncome={}",
                record.getRecordId(), order.getOrderId(), actualRefund, sellerIncome);
        return actualRefund;
    }

    private BigDecimal settleErrandAfterDispute(Long operatorId,
                                                DisputeRecord record,
                                                boolean resolved,
                                                BigDecimal refundAmount) {
        ErrandTask task = errandTaskMapper.selectById(record.getContentId());
        if (task == null) {
            return BigDecimal.ZERO;
        }

        if (ErrandStatus.COMPLETED.getCode().equals(task.getTaskStatus())
                || ErrandStatus.CANCELLED.getCode().equals(task.getTaskStatus())) {
            log.info("跑腿任务已是终态，跳过纠纷结算: disputeId={}, taskId={}, status={}",
                    record.getRecordId(), task.getTaskId(), task.getTaskStatus());
            return BigDecimal.ZERO;
        }

        BigDecimal actualRefund = BigDecimal.ZERO;
        if (resolved
                && record.getClaimRefund() != null
                && record.getClaimRefund() == 1
                && refundAmount != null
                && refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal maxAmount = task.getReward();
            if (record.getClaimRefundAmount() != null && record.getClaimRefundAmount().compareTo(maxAmount) < 0) {
                maxAmount = record.getClaimRefundAmount();
            }
            if (refundAmount.compareTo(maxAmount) > 0) {
                throw new BusinessException("退款金额不能超过可裁定金额");
            }
            actualRefund = refundAmount;
        }

        if (actualRefund.compareTo(BigDecimal.ZERO) > 0) {
            UserInfo publisher = userInfoMapper.selectById(task.getPublisherId());
            if (publisher != null) {
                publisher.setMoney(publisher.getMoney().add(actualRefund));
                userInfoMapper.updateById(publisher);
            }
        }

        BigDecimal acceptorIncome = task.getReward().subtract(actualRefund);
        if (acceptorIncome.compareTo(BigDecimal.ZERO) > 0 && task.getAcceptorId() != null) {
            UserInfo acceptor = userInfoMapper.selectById(task.getAcceptorId());
            if (acceptor != null) {
                acceptor.setMoney(acceptor.getMoney().add(acceptorIncome));
                userInfoMapper.updateById(acceptor);
            }
        }

        if (actualRefund.compareTo(task.getReward()) == 0) {
            task.setTaskStatus(ErrandStatus.CANCELLED.getCode());
            task.setCancelTime(LocalDateTime.now());
            if (StrUtil.isBlank(task.getCancelReason())) {
                task.setCancelReason("纠纷裁定全额退款");
            }
        } else {
            task.setTaskStatus(ErrandStatus.COMPLETED.getCode());
            if (task.getConfirmTime() == null) {
                task.setConfirmTime(LocalDateTime.now());
            }
        }
        errandTaskMapper.updateById(task);

        log.info("跑腿纠纷结算完成: disputeId={}, taskId={}, refund={}, acceptorIncome={}",
                record.getRecordId(), task.getTaskId(), actualRefund, acceptorIncome);
        return actualRefund;
    }

    private void sendGoodsSyncAfterCommit(GoodsSyncMessage message) {
        if (message == null) {
            return;
        }
        Runnable syncTask = () -> {
            try {
                rocketMQTemplate.convertAndSend(RocketMQConfig.GOODS_SYNC_TOPIC, message);
            } catch (Exception e) {
                log.error("发送商品同步消息失败: type={}, goodsId={}", message.getType(), message.getProductId(), e);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            syncTask.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncTask.run();
            }
        });
    }

    public Page<DisputeVO> getDisputeList(Long operatorId,
                                          PageQuery query,
                                          Integer status,
                                          Integer targetType,
                                          String schoolCode,
                                          String campusCode) {
        List<IamAdminScopeBinding> scopes = scopeSupport.getOperatorScopes(operatorId);

        Page<DisputeRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<DisputeRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(DisputeRecord::getHandleStatus, status);
        }
        if (targetType != null) {
            wrapper.eq(DisputeRecord::getTargetType, targetType);
        }
        wrapper.eq(StrUtil.isNotBlank(schoolCode), DisputeRecord::getSchoolCode, schoolCode)
                .eq(StrUtil.isNotBlank(campusCode), DisputeRecord::getCampusCode, campusCode);
        wrapper.orderByDesc(DisputeRecord::getCreateTime);
        scopeSupport.applyScopeFilter(wrapper, scopes, DisputeRecord::getSchoolCode, DisputeRecord::getCampusCode);

        Page<DisputeRecord> disputePage = disputeRecordMapper.selectPage(page, wrapper);

        List<DisputeRecord> records = disputePage.getRecords();
        if (records.isEmpty()) {
            return new Page<DisputeVO>(disputePage.getCurrent(), disputePage.getSize(), disputePage.getTotal());
        }

        List<Long> userIds = records.stream()
                .flatMap(d -> List.of(d.getInitiatorId(), d.getRelatedId()).stream())
                .distinct()
                .toList();
        Map<Long, UserInfo> userMap = userInfoMapper.selectBatchIds(userIds).stream()
                .collect(java.util.stream.Collectors.toMap(UserInfo::getUserId, u -> u));

        List<Long> orderIds = records.stream()
                .map(DisputeRecord::getContentId)
                .distinct()
                .toList();
        Map<Long, OrderInfo> orderMap = orderInfoMapper.selectBatchIds(orderIds).stream()
                .collect(java.util.stream.Collectors.toMap(OrderInfo::getOrderId, o -> o));

        Map<String, SchoolInfo> schoolMap = schoolInfoSupport.buildSchoolInfoMap(records, DisputeRecord::getSchoolCode);

        List<DisputeVO> vos = records.stream()
                .map(record -> {
                    DisputeVO vo = BeanUtil.copyProperties(record, DisputeVO.class);
                    UserInfo initiator = userMap.get(record.getInitiatorId());
                    if (initiator != null) {
                        vo.setInitiatorName(initiator.getNickName());
                        vo.setInitiatorAvatar(initiator.getImageUrl());
                    }
                    UserInfo related = userMap.get(record.getRelatedId());
                    if (related != null) {
                        vo.setRelatedName(related.getNickName());
                        vo.setRelatedAvatar(related.getImageUrl());
                    }
                    OrderInfo order = orderMap.get(record.getContentId());
                    if (order != null) {
                        vo.setOrderNo(order.getOrderNo());
                    }
                    schoolInfoSupport.fillSchoolCampusNames(
                            record.getSchoolCode(),
                            record.getCampusCode(),
                            schoolMap,
                            vo::setSchoolName,
                            vo::setCampusName
                    );
                    return vo;
                })
                .toList();

        return new Page<DisputeVO>(disputePage.getCurrent(), disputePage.getSize(), disputePage.getTotal()).setRecords(vos);
    }
}
