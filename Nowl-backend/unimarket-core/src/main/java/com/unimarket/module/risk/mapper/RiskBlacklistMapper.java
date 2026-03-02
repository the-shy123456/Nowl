package com.unimarket.module.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.risk.entity.RiskBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风控黑名单 Mapper
 */
@Mapper
public interface RiskBlacklistMapper extends BaseMapper<RiskBlacklist> {
}

