package com.unimarket.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.recommend.entity.UserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户偏好Mapper
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreference> {
}
