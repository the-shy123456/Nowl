package com.unimarket.module.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IAM角色实体
 */
@Data
@TableName("iam_role")
public class IamRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;

    private String roleCode;

    private String roleName;

    private Integer roleLevel;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
