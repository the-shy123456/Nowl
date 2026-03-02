package com.unimarket.module.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.risk.entity.RiskWhitelist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控白名单 Mapper
 */
@Mapper
public interface RiskWhitelistMapper extends BaseMapper<RiskWhitelist> {
}

