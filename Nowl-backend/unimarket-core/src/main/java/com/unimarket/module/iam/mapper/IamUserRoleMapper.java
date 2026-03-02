package com.unimarket.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.iam.entity.IamUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色绑定 Mapper
 */
@Mapper
public interface IamUserRoleMapper extends BaseMapper<IamUserRole> {
}

