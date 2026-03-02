package com.unimarket.module.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员数据范围绑定实体
 */
@Data
@TableName("iam_admin_scope_binding")
public class IamAdminScopeBinding implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "binding_id", type = IdType.AUTO)
    private Long bindingId;

    private Long userId;

    private String scopeType;

    private String schoolCode;

    private String campusCode;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
