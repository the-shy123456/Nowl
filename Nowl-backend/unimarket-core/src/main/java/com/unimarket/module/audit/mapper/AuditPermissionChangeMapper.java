package com.unimarket.module.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.audit.entity.AuditPermissionChange;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限变更审计 Mapper
 */
@Mapper
public interface AuditPermissionChangeMapper extends BaseMapper<AuditPermissionChange> {
}

