package com.unimarket.module.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.risk.entity.RiskRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控规则 Mapper
 */
@Mapper
public interface RiskRuleMapper extends BaseMapper<RiskRule> {
}

