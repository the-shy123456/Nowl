package com.unimarket.module.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注关系实体类
 */
@Data
@TableName("user_follow")
public class UserFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关注记录主键ID
     */
    @TableId(value = "follow_id", type = IdType.AUTO)
    private Long followId;

    /**
     * 关注人ID（主动关注方）
     */
    private Long userId;

    /**
     * 被关注人ID（被动被关注方）
     */
    private Long followedUserId;

    /**
     * 关注时间
     */
    private LocalDateTime followTime;

    /**
     * 是否取消关注（0-未取消，1-已取消）
     */
    private Integer isCancel;

    /**
     * 记录创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
