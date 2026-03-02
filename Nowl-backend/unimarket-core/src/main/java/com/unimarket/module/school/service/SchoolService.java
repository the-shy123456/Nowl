package com.unimarket.module.school.service;

import com.unimarket.module.school.vo.SchoolVO;

import java.util.List;

/**
 * 学校Service接口
 */
public interface SchoolService {

    /**
     * 查询学校列表
     */
    List<SchoolVO> getSchoolList();

    /**
     * 根据学校编码查询校区列表
     */
    List<SchoolVO> getCampusList(String schoolCode);
}
