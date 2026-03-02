package com.unimarket.admin.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 登录轨迹查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginTraceQueryDTO extends PageQuery {

    private Long userId;

    private String phone;

    private String loginResult;

    private String riskLevel;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}

