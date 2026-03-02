package com.unimarket.module.dispute.service;

import com.unimarket.module.dispute.dto.OrderDisputeApplyDTO;

/**
 * 订单纠纷申请服务
 */
public interface OrderDisputeService {

    /**
     * 发起订单纠纷
     */
    void applyDispute(Long userId, OrderDisputeApplyDTO dto);
}

