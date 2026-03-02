package com.unimarket.module.risk.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 风控事件
 */
@Data
@TableName("risk_event")
public class RiskEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "event_id", type = IdType.AUTO)
    private Long eventId;

    private String traceId;

    private String eventType;

    private String subjectType;

    private String subjectId;

    private String schoolCode;

    private String campusCode;

    private String riskFeatures;

    private String rawPayload;

    private LocalDateTime eventTime;
}

