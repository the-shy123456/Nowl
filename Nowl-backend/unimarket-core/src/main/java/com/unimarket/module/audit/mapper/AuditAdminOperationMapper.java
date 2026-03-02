package com.unimarket.module.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.audit.entity.AuditAdminOperation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台操作审计 Mapper
 */
@Mapper
public interface AuditAdminOperationMapper extends BaseMapper<AuditAdminOperation> {
}

