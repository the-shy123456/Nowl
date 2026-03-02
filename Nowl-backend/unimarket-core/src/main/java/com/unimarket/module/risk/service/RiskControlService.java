package com.unimarket.module.risk.service;

import com.unimarket.module.risk.dto.RiskContext;
import com.unimarket.module.risk.vo.RiskDecisionResult;

/**
 * 统一风控服务
 */
public interface RiskControlService {

    /**
     * 评估风控，返回决策
     */
    RiskDecisionResult evaluate(RiskContext context);

    /**
     * 风控拦截入口：非 ALLOW 直接抛出业务异常
     */
    void assertAllowed(RiskContext context);
}

