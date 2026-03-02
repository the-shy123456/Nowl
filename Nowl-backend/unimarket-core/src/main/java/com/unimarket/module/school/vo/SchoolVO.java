package com.unimarket.module.school.vo;

import lombok.Data;

/**
 * 学校与校区返回对象
 */
@Data
public class SchoolVO {

    private String schoolCode;

    private String schoolName;

    private String campusCode;

    private String campusName;

    private Integer status;
}
