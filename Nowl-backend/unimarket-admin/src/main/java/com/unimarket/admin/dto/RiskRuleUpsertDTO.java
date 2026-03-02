package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 风控规则新增/更新参数
 */
@Data
public class RiskRuleUpsertDTO {

    private Long ruleId;

    @NotBlank(message = "规则编码不能为空")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    /**
     * THRESHOLD / KEYWORD
     */
    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    /**
     * JSON 字符串
     */
    @NotBlank(message = "规则配置不能为空")
    private String ruleConfig;

    /**
     * ALLOW / REJECT / CHALLENGE / REVIEW / LIMIT
     */
    @NotBlank(message = "决策动作不能为空")
    private String decisionAction;

    private Integer priority = 100;
}

