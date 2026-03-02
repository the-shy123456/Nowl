package com.unimarket.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.user.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * UserInfo Mapper接口
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    /**
     * 原子更新用户信用分
     * @param userId 用户ID
     * @param change 变化值（正数为加，负数为减）
     * @param minScore 最小分数限制
     * @param maxScore 最大分数限制
     * @return 影响行数
     */
    @Update("UPDATE user_info SET credit_score = " +
            "CASE " +
            "WHEN (credit_score + #{change}) < #{minScore} THEN #{minScore} " +
            "WHEN (credit_score + #{change}) > #{maxScore} THEN #{maxScore} " +
            "ELSE (credit_score + #{change}) END, " +
            "update_time = NOW() " +
            "WHERE user_id = #{userId}")
    int updateCreditScore(@Param("userId") Long userId, 
                          @Param("change") int change, 
                          @Param("minScore") int minScore, 
                          @Param("maxScore") int maxScore);
}
