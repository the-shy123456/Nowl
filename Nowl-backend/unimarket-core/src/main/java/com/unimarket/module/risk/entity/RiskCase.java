package com.unimarket.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控工单
 */
@Data
@TableName("risk_case")
public class RiskCase implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "case_id", type = IdType.AUTO)
    private Long caseId;

    private Long eventId;

    private Long decisionId;

    private String schoolCode;

    private String campusCode;

    private String caseStatus;

    private Long assigneeId;

    private String result;

    private String resultReason;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
