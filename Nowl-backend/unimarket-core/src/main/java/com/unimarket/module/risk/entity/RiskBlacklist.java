package com.unimarket.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控黑名单
 */
@Data
@TableName("risk_blacklist")
public class RiskBlacklist implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String subjectType;

    private String subjectId;

    private String reason;

    private String source;

    private LocalDateTime expireTime;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

