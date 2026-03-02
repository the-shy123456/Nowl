package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限变更审计视图
 */
@Data
public class PermissionChangeVO {

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

