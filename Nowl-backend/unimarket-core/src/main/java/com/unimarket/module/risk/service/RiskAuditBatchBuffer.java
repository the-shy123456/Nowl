package com.unimarket.module.risk.service;

import com.unimarket.module.risk.dto.RiskAuditMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 风控审计批量缓冲区。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAuditBatchBuffer {

    private static final int BATCH_SIZE = 200;

    private final Queue<RiskAuditMessage> queue = new ConcurrentLinkedQueue<>();
    private final RiskAuditPersistenceService riskAuditPersistenceService;

    public void enqueue(RiskAuditMessage message) {
        if (message == null) {
            return;
        }
        queue.offer(message);
        if (queue.size() >= BATCH_SIZE) {
            flushOnce();
        }
    }

    @Scheduled(fixedDelay = 1000L)
    public void flushScheduled() {
        flushOnce();
    }

    public synchronized void flushOnce() {
        if (queue.isEmpty()) {
            return;
        }
        List<RiskAuditMessage> batch = new ArrayList<>(BATCH_SIZE);
        while (batch.size() < BATCH_SIZE) {
            RiskAuditMessage message = queue.poll();
            if (message == null) {
                break;
            }
            batch.add(message);
        }
        if (batch.isEmpty()) {
            return;
        }

        try {
            riskAuditPersistenceService.persistBatch(batch);
        } catch (Exception ex) {
            log.error("风控审计批量落库失败，准备回退到缓冲队列，batchSize={}", batch.size(), ex);
            for (RiskAuditMessage item : batch) {
                queue.offer(item);
            }
        }
    }
}
