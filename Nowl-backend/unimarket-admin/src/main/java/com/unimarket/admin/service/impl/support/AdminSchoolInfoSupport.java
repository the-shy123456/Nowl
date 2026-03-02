package com.unimarket.admin.service.impl.support;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学校/校区信息补全能力封装（批量查询，避免 N+1）。
 */
@Component
@RequiredArgsConstructor
public class AdminSchoolInfoSupport {

    private final SchoolInfoMapper schoolInfoMapper;

    private String buildSchoolCampusKey(String schoolCode, String campusCode) {
        return (schoolCode == null ? "" : schoolCode) + "|" + (campusCode == null ? "" : campusCode);
    }

    public <T> Map<String, SchoolInfo> buildSchoolInfoMap(List<T> records, Function<T, String> schoolCodeGetter) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> schoolCodes = records.stream()
                .map(schoolCodeGetter)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        if (schoolCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SchoolInfo> schools = schoolInfoMapper.selectList(new LambdaQueryWrapper<SchoolInfo>()
                .in(SchoolInfo::getSchoolCode, schoolCodes));
        return schools.stream().collect(Collectors.toMap(
                s -> buildSchoolCampusKey(s.getSchoolCode(), s.getCampusCode()),
                Function.identity(),
                (a, b) -> a
        ));
    }

    private SchoolInfo findSchoolInfo(Map<String, SchoolInfo> schoolMap, String schoolCode, String campusCode) {
        if (schoolMap == null || schoolMap.isEmpty() || StrUtil.isBlank(schoolCode)) {
            return null;
        }
        SchoolInfo exact = schoolMap.get(buildSchoolCampusKey(schoolCode, campusCode));
        if (exact != null) {
            return exact;
        }
        for (SchoolInfo schoolInfo : schoolMap.values()) {
            if (schoolCode.equals(schoolInfo.getSchoolCode())) {
                return schoolInfo;
            }
        }
        return null;
    }

    public void fillSchoolCampusNames(String schoolCode,
                                      String campusCode,
                                      Map<String, SchoolInfo> schoolMap,
                                      Consumer<String> schoolNameSetter,
                                      Consumer<String> campusNameSetter) {
        SchoolInfo schoolInfo = findSchoolInfo(schoolMap, schoolCode, campusCode);
        if (schoolInfo == null) {
            return;
        }
        schoolNameSetter.accept(schoolInfo.getSchoolName());
        campusNameSetter.accept(schoolInfo.getCampusName());
    }
}

