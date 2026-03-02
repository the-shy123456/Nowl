package com.unimarket.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.iam.entity.IamRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * IAM角色Mapper
 */
@Mapper
public interface IamRoleMapper extends BaseMapper<IamRole> {

    /**
     * 查询用户有效角色编码
     */
    @Select("""
            SELECT DISTINCT r.role_code
            FROM iam_user_role ur
            JOIN iam_role r ON ur.role_id = r.role_id
            WHERE ur.user_id = #{userId}
              AND ur.status = 1
              AND r.status = 1
              AND (ur.expired_time IS NULL OR ur.expired_time > NOW())
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);
}
