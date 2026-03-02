package com.unimarket.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户行为管控规则
 */
@Data
@TableName("risk_behavior_control")
public class RiskBehaviorControl implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String eventType;

    private String controlAction;

    private String reason;

    private LocalDateTime expireTime;

    private Integer status;

    private Long operatorId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

