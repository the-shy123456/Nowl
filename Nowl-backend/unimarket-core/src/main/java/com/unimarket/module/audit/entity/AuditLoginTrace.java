package com.unimarket.module.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录审计轨迹
 */
@Data
@TableName("audit_login_trace")
public class AuditLoginTrace {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String traceId;

    private Long userId;

    private String phone;

    private String ip;

    private String deviceId;

    private String geo;

    private String loginResult;

    private String failReason;

    private String riskLevel;

    private LocalDateTime createTime;
}

