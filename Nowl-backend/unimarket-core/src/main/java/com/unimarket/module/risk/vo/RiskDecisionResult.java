package com.unimarket.module.risk.vo;

import com.unimarket.module.risk.enums.RiskAction;
import lombok.Builder;
import lombok.Data;

/**
 * 风控决策结果
 */
@Data
@Builder
public class RiskDecisionResult {

    private Long eventId;

    private Long decisionId;

    private RiskAction action;

    private String riskLevel;

    private Double riskScore;

    private String reason;
}

