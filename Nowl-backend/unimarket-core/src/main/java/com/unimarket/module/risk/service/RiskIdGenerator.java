package com.unimarket.module.risk.service;

import cn.hutool.core.util.IdUtil;
import org.springframework.stereotype.Service;

/**
 * 风控主键生成器。
 */
@Service
public class RiskIdGenerator {

    public Long nextId() {
        return IdUtil.getSnowflakeNextId();
    }
}
