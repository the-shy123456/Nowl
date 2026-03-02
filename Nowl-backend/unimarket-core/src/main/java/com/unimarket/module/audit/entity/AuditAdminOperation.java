package com.unimarket.module.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台操作审计日志
 */
@Data
@TableName("audit_admin_operation")
public class AuditAdminOperation {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long operatorId;

    private String operatorIp;

    private String module;

    private String action;

    private String targetType;

    private String targetId;

    private String requestPayload;

    private String resultStatus;

    private String resultMessage;

    private Integer costMs;

    private LocalDateTime createTime;
}

