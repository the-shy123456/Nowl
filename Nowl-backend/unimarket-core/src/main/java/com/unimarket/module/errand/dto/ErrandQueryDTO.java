package com.unimarket.module.errand.dto;

import com.unimarket.common.result.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 跑腿任务查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErrandQueryDTO extends PageQuery {
    
    /**
     * 任务状态
     */
    private Integer taskStatus;
    
    /**
     * 关键词
     */
    private String keyword;

    /**
     * 学校编码
     */
    private String schoolCode;

    /**
     * 校区编码
     */
    private String campusCode;
}
