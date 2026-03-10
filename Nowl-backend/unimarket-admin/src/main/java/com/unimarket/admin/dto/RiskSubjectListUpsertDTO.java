package com.unimarket.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控名单新增/更新请求。
 */
@Data
public class RiskSubjectListUpsertDTO {

    @NotBlank(message = "主体类型不能为空")
    private String subjectType;

    @NotBlank(message = "主体标识不能为空")
    private String subjectId;

    private String reason;

    private LocalDateTime expireTime;
}
