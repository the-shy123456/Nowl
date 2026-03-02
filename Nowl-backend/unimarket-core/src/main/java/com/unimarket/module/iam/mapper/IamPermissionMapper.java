package com.unimarket.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.iam.entity.IamPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * IAM权限Mapper
 */
@Mapper
public interface IamPermissionMapper extends BaseMapper<IamPermission> {

    /**
     * 查询用户有效权限编码
     */
    @Select("""
            SELECT DISTINCT p.permission_code
            FROM iam_user_role ur
            JOIN iam_role r ON ur.role_id = r.role_id
            JOIN iam_role_permission rp ON rp.role_id = r.role_id
            JOIN iam_permission p ON p.permission_id = rp.permission_id
            WHERE ur.user_id = #{userId}
              AND ur.status = 1
              AND r.status = 1
              AND rp.status = 1
              AND p.status = 1
              AND (ur.expired_time IS NULL OR ur.expired_time > NOW())
            """)
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId);
}
