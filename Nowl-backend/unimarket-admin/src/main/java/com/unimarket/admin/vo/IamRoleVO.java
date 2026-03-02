package com.unimarket.admin.vo;

import lombok.Data;

/**
 * IAM角色视图
 */
@Data
public class IamRoleVO {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private Integer roleLevel;

    private Integer status;
}

