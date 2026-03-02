package com.unimarket.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.recommend.entity.UserBehaviorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户行为日志Mapper
 */
@Mapper
public interface UserBehaviorLogMapper extends BaseMapper<UserBehaviorLog> {

    /**
     * 查询用户最近的行为商品ID
     */
    @Select("SELECT t.product_id FROM (" +
            "SELECT product_id, MAX(create_time) AS last_time " +
            "FROM user_behavior_log " +
            "WHERE user_id = #{userId} AND product_id IS NOT NULL " +
            "GROUP BY product_id" +
            ") t ORDER BY t.last_time DESC LIMIT #{limit}")
    List<Long> selectRecentProductIds(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询用户的分类偏好统计
     */
    @Select("SELECT category_id, SUM(CASE behavior_type " +
            "WHEN 1 THEN 1 WHEN 2 THEN 3 WHEN 3 THEN 5 ELSE 0 END) as score " +
            "FROM user_behavior_log " +
            "WHERE user_id = #{userId} AND category_id IS NOT NULL " +
            "GROUP BY category_id ORDER BY score DESC")
    List<Map<String, Object>> selectCategoryPreference(@Param("userId") Long userId);

    /**
     * 查询对同一商品有行为的其他用户
     */
    @Select("SELECT DISTINCT user_id FROM user_behavior_log " +
            "WHERE product_id = #{productId} AND user_id != #{userId}")
    List<Long> selectUsersWithSameProduct(@Param("productId") Long productId, @Param("userId") Long userId);

    /**
     * 查询两个商品的共同用户行为数
     */
    @Select("SELECT COUNT(DISTINCT a.user_id) FROM user_behavior_log a " +
            "JOIN user_behavior_log b ON a.user_id = b.user_id " +
            "WHERE a.product_id = #{productId1} AND b.product_id = #{productId2}")
    Integer selectCommonUserCount(@Param("productId1") Long productId1, @Param("productId2") Long productId2);

    /**
     * 查询商品的用户行为数
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM user_behavior_log WHERE product_id = #{productId}")
    Integer selectProductUserCount(@Param("productId") Long productId);

    /**
     * 查询热门商品（按行为加权）
     */
    @Select("SELECT product_id, SUM(CASE behavior_type " +
            "WHEN 1 THEN 1 WHEN 2 THEN 3 WHEN 3 THEN 5 ELSE 0 END) as hot_score " +
            "FROM user_behavior_log " +
            "WHERE product_id IS NOT NULL AND create_time > DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY product_id ORDER BY hot_score DESC LIMIT #{limit}")
    List<Map<String, Object>> selectHotProducts(@Param("limit") int limit);
}
