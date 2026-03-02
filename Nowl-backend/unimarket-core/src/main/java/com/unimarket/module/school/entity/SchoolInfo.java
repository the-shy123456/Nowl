package com.unimarket.module.school.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 学校信息实体类
 */
@Data
@TableName("school_info")
public class SchoolInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 学校标准编码（主键）
     */
    @TableId
    private String schoolCode;

    /**
     * 学校中文全称
     */
    private String schoolName;

    /**
     * 校区编号（联合主键）
     */
    private String campusCode;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 状态（0-禁用，1-启用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
