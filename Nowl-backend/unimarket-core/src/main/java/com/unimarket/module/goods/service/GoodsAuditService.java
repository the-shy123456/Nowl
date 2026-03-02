package com.unimarket.module.goods.service;

/**
 * 商品审核Service
 * 由 MQ 消费者调用
 */
public interface GoodsAuditService {
    /**
     * 审核商品
     * @param goodsId 商品ID
     * @param operationType 操作类型：1-新增，2-更新
     */
    void performAudit(Long goodsId, int operationType);
}
