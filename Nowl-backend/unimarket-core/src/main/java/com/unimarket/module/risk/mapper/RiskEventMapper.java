package com.unimarket.module.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.risk.entity.RiskEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控事件 Mapper
 */
@Mapper
public interface RiskEventMapper extends BaseMapper<RiskEvent> {
}

