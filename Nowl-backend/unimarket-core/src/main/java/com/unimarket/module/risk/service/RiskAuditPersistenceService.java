package com.unimarket.module.risk.service;

import com.unimarket.module.risk.dto.RiskAuditMessage;
import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import com.unimarket.module.risk.mapper.RiskAuditBatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 风控审计持久化服务。
 */
@Service
@RequiredArgsConstructor
public class RiskAuditPersistenceService {

    private final RiskAuditBatchMapper riskAuditBatchMapper;

    @Transactional(rollbackFor = Exception.class)
    public void persistBatch(List<RiskAuditMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<RiskEvent> events = messages.stream()
                .map(RiskAuditMessage::getEvent)
                .filter(item -> item != null)
                .toList();
        if (!events.isEmpty()) {
            riskAuditBatchMapper.batchInsertEvents(events);
        }

        List<RiskDecision> decisions = messages.stream()
                .map(RiskAuditMessage::getDecision)
                .filter(item -> item != null)
                .toList();
        if (!decisions.isEmpty()) {
            riskAuditBatchMapper.batchInsertDecisions(decisions);
        }

        List<RiskCase> riskCases = messages.stream()
                .map(RiskAuditMessage::getRiskCase)
                .filter(item -> item != null)
                .toList();
        if (!riskCases.isEmpty()) {
            riskAuditBatchMapper.batchInsertCases(riskCases);
        }
    }
}
