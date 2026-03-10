package com.unimarket.admin.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控黑白名单项。
 */
@Data
public class RiskSubjectListVO {

    private Long id;

    private String subjectType;

    private String subjectId;

    private String reason;

    private String source;

    private LocalDateTime expireTime;

    private Integer status;

    private LocalDateTime createTime;
}
