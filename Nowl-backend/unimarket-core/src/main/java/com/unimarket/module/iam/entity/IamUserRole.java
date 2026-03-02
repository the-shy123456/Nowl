package com.unimarket.module.iam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IAM用户角色关联实体
 */
@Data
@TableName("iam_user_role")
public class IamUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long roleId;

    private Integer status;

    private LocalDateTime expiredTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
