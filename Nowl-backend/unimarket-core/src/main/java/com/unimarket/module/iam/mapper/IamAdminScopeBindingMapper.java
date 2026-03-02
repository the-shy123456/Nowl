package com.unimarket.module.iam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.iam.entity.IamAdminScopeBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 管理员数据范围Mapper
 */
@Mapper
public interface IamAdminScopeBindingMapper extends BaseMapper<IamAdminScopeBinding> {

    /**
     * 查询管理员有效范围绑定
     */
    @Select("""
            SELECT binding_id, user_id, scope_type, school_code, campus_code, status, create_time, update_time
            FROM iam_admin_scope_binding
            WHERE user_id = #{userId}
              AND status = 1
            """)
    List<IamAdminScopeBinding> selectActiveByUserId(@Param("userId") Long userId);
}
