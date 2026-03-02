package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控规则视图
 */
@Data
public class RiskRuleVO {

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private String eventType;

    private String ruleType;

    private String ruleConfig;

    private String decisionAction;

    private Integer priority;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

