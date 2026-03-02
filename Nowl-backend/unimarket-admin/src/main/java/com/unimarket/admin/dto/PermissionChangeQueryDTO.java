package com.unimarket.admin.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 权限变更审计查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionChangeQueryDTO extends PageQuery {

    private Long operatorId;

    private Long targetUserId;

    private String changeType;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}

