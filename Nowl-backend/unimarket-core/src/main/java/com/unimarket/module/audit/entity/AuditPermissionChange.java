package com.unimarket.module.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限变更审计
 */
@Data
@TableName("audit_permission_change")
public class AuditPermissionChange {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long operatorId;

    private String changeType;

    private Long targetUserId;

    private Long targetRoleId;

    private Long targetPermissionId;

    private String beforeData;

    private String afterData;

    private String reason;

    private LocalDateTime createTime;
}

