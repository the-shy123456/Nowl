package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色绑定视图
 */
@Data
public class UserRoleBindingVO {

    private Long id;

    private Long userId;

    private Long roleId;

    private String roleCode;

    private String roleName;

    private Integer status;

    private LocalDateTime expiredTime;

    private LocalDateTime createTime;
}

