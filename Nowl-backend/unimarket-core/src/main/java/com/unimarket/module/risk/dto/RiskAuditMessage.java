package com.unimarket.module.risk.dto;

import com.unimarket.module.risk.entity.RiskCase;
import com.unimarket.module.risk.entity.RiskDecision;
import com.unimarket.module.risk.entity.RiskEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 风控审计消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAuditMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private RiskEvent event;

    private RiskDecision decision;

    private RiskCase riskCase;

    /**
     * Redis 兜底重试次数。
     */
    @Builder.Default
    private Integer mqRetryCount = 0;
}
