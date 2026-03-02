package com.unimarket.module.school.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.constant.CacheConstants;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.school.entity.SchoolInfo;
import com.unimarket.module.school.mapper.SchoolInfoMapper;
import com.unimarket.module.school.service.SchoolService;
import com.unimarket.module.school.vo.SchoolVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 学校Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolInfoMapper schoolInfoMapper;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;

    private static final String CAMPUS_LIST_CACHE_KEY = "campus:list:";

    @Override
    public List<SchoolVO> getSchoolList() {
        // 1. 先从缓存获取
        List<SchoolInfo> cachedList = redisCache.getCacheList(CacheConstants.SCHOOL_INFO);
        if (cachedList != null) {
            return toSchoolVOList(cachedList);
        }

        // 2. 缓存失效，加锁防止击穿
        String lockKey = "lock:school:list";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                // 双重检查
                cachedList = redisCache.getCacheList(CacheConstants.SCHOOL_INFO);
                if (cachedList != null) {
                    return toSchoolVOList(cachedList);
                }

                // 3. 查询数据库
                LambdaQueryWrapper<SchoolInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(SchoolInfo::getStatus, 1)
                        .orderBy(true, true, SchoolInfo::getSchoolCode);
                
                List<SchoolInfo> list = schoolInfoMapper.selectList(wrapper);
                
                // 去重
                List<SchoolInfo> schoolList = list.stream()
                        .collect(Collectors.toMap(
                                SchoolInfo::getSchoolCode,
                                school -> school,
                                (existing, replacement) -> existing))
                        .values()
                        .stream()
                        .collect(Collectors.toList());

                // 4. 写入缓存（使用随机 TTL 防雪崩）
                redisCache.setWithJitter(CacheConstants.SCHOOL_INFO, schoolList, CacheConstants.SCHOOL_INFO_EXPIRE, 10);
                return toSchoolVOList(schoolList);
            } else {
                log.warn("未能获取缓存锁，降级查询数据库: {}", lockKey);
                List<SchoolInfo> list = schoolInfoMapper.selectList(
                        new LambdaQueryWrapper<SchoolInfo>()
                                .eq(SchoolInfo::getStatus, 1)
                                .orderBy(true, true, SchoolInfo::getSchoolCode)
                );
                List<SchoolInfo> schoolList = list.stream()
                        .collect(Collectors.toMap(
                                SchoolInfo::getSchoolCode,
                                school -> school,
                                (existing, replacement) -> existing))
                        .values()
                        .stream()
                        .collect(Collectors.toList());
                return toSchoolVOList(schoolList);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙，请稍后重试");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<SchoolVO> getCampusList(String schoolCode) {
        // 先从缓存获取
        String cacheKey = CAMPUS_LIST_CACHE_KEY + schoolCode;
        List<SchoolInfo> cachedList = redisCache.getCacheList(cacheKey);
        if (cachedList != null && !cachedList.isEmpty()) {
            return toSchoolVOList(cachedList);
        }

        // 查询数据库 - 查询该学校下所有状态正常的校区
        LambdaQueryWrapper<SchoolInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SchoolInfo::getSchoolCode, schoolCode)
                .eq(SchoolInfo::getStatus, 1)
                .orderBy(true, true, SchoolInfo::getCampusCode);
        
        List<SchoolInfo> list = schoolInfoMapper.selectList(wrapper);


        // 缓存1天 (添加随机抖动防雪崩)
        redisCache.setWithJitter(cacheKey, list, CacheConstants.SCHOOL_INFO_EXPIRE, 15);

        return toSchoolVOList(list);
    }

    private List<SchoolVO> toSchoolVOList(List<SchoolInfo> list) {
        return list.stream()
                .map(this::toSchoolVO)
                .collect(Collectors.toList());
    }

    private SchoolVO toSchoolVO(SchoolInfo schoolInfo) {
        SchoolVO vo = new SchoolVO();
        vo.setSchoolCode(schoolInfo.getSchoolCode());
        vo.setSchoolName(schoolInfo.getSchoolName());
        vo.setCampusCode(schoolInfo.getCampusCode());
        vo.setCampusName(schoolInfo.getCampusName());
        vo.setStatus(schoolInfo.getStatus());
        return vo;
    }
}
