package com.unimarket.module.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.audit.entity.AuditLoginTrace;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录审计 Mapper
 */
@Mapper
public interface AuditLoginTraceMapper extends BaseMapper<AuditLoginTrace> {
}

