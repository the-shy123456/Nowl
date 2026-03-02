package com.unimarket.module.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IAM权限点实体
 */
@Data
@TableName("iam_permission")
public class IamPermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;

    private String permissionCode;

    private String permissionName;

    private String permissionGroup;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
