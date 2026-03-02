package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员范围绑定视图
 */
@Data
public class AdminScopeBindingVO {

    private Long bindingId;

    private Long userId;

    private String scopeType;

    private String schoolCode;

    private String campusCode;

    private Integer status;

    private LocalDateTime createTime;
}

