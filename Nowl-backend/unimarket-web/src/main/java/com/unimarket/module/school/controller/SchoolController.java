package com.unimarket.module.school.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.school.service.SchoolService;
import com.unimarket.module.school.vo.SchoolVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学校Controller
 */
@Slf4j
@RestController
@RequestMapping("/school")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    /**
     * 查询学校列表
     */
    @GetMapping("/list")
    public Result<List<SchoolVO>> list() {
        List<SchoolVO> list = schoolService.getSchoolList();
        return Result.success(list);
    }

    /**
     * 根据学校编码查询校区列表
     */
    @GetMapping("/{schoolCode}/campus")
    public Result<List<SchoolVO>> campusList(@PathVariable String schoolCode) {
        List<SchoolVO> list = schoolService.getCampusList(schoolCode);
        return Result.success(list);
    }
}
