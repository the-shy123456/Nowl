package com.unimarket.admin.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 风控名单查询参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RiskSubjectListQueryDTO extends PageQuery {

    private String subjectType;

    private String subjectId;
}
