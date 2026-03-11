package com.unimarket.module.errand.service;

/**
 * 跑腿任务审核服务
 */
public interface ErrandAuditService {

    /**
     * 异步审核跑腿任务（由 MQ 消费者触发）
     *
     * @param taskId        任务ID
     * @param operationType 操作类型（1-新增，2-更新）
     */
    void performAudit(Long taskId, int operationType);
}
