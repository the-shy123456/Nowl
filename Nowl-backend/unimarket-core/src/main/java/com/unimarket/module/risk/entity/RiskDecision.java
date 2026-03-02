package com.unimarket.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控决策
 */
@Data
@TableName("risk_decision")
public class RiskDecision implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "decision_id", type = IdType.AUTO)
    private Long decisionId;

    private Long eventId;

    private String decisionAction;

    private String riskLevel;

    private BigDecimal riskScore;

    private String matchedRuleCodes;

    private String decisionReason;

    private LocalDateTime createTime;
}

