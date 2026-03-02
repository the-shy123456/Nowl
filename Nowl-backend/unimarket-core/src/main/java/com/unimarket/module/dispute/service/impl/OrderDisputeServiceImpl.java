package com.unimarket.module.dispute.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.enums.DisputeStatus;
import com.unimarket.common.enums.DisputeTargetType;
import com.unimarket.common.enums.OrderStatus;
import com.unimarket.common.enums.RefundStatus;
import com.unimarket.common.exception.BusinessException;
import com.unimarket.module.dispute.dto.OrderDisputeApplyDTO;
import com.unimarket.module.dispute.entity.DisputeRecord;
import com.unimarket.module.dispute.mapper.DisputeRecordMapper;
import com.unimarket.module.dispute.service.OrderDisputeService;
import com.unimarket.module.order.entity.OrderInfo;
import com.unimarket.module.order.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单纠纷申请服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDisputeServiceImpl implements OrderDisputeService {

    private final OrderInfoMapper orderInfoMapper;
    private final DisputeRecordMapper disputeRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyDispute(Long userId, OrderDisputeApplyDTO dto) {
        OrderInfo order = orderInfoMapper.selectById(dto.getOrderId());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        boolean isParticipant = userId.equals(order.getBuyerId()) || userId.equals(order.getSellerId());
        if (!isParticipant) {
            throw new BusinessException("无权对该订单发起纠纷");
        }
        boolean disputeWindowOpen = OrderStatus.PENDING_RECEIVE.getCode().equals(order.getOrderStatus());
        if (!disputeWindowOpen) {
            throw new BusinessException("当前订单状态不支持发起纠纷");
        }
        if (RefundStatus.PENDING.getCode().equals(order.getRefundStatus())) {
            throw new BusinessException("订单退款处理中，暂不可发起纠纷");
        }
        LambdaQueryWrapper<DisputeRecord> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(DisputeRecord::getContentId, dto.getOrderId())
                .eq(DisputeRecord::getTargetType, DisputeTargetType.ORDER.getCode())
                .in(DisputeRecord::getHandleStatus, DisputeStatus.PENDING.getCode(), DisputeStatus.PROCESSING.getCode());
        if (disputeRecordMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessException("该订单已存在进行中的纠纷，请勿重复发起");
        }

        Long relatedId = order.getBuyerId().equals(userId) ? order.getSellerId() : order.getBuyerId();

        DisputeRecord record = new DisputeRecord();
        record.setInitiatorId(userId);
        record.setRelatedId(relatedId);
        record.setContentId(dto.getOrderId());
        record.setTargetType(DisputeTargetType.ORDER.getCode());
        record.setSchoolCode(order.getSchoolCode());
        record.setCampusCode(order.getCampusCode());
        record.setContent(dto.getReason());
        record.setEvidenceUrls(dto.getEvidenceImages());
        record.setHandleStatus(DisputeStatus.PENDING.getCode());

        disputeRecordMapper.insert(record);
        log.info("用户发起订单纠纷: userId={}, orderId={}", userId, dto.getOrderId());
    }
}
