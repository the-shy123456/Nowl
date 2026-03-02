package com.unimarket.module.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.unimarket.module.review.entity.ReviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 评价记录Mapper
 */
@Mapper
public interface ReviewRecordMapper extends BaseMapper<ReviewRecord> {

    /**
     * 计算用户的平均评分
     */
    @Select("SELECT AVG(rating) FROM review_record WHERE reviewed_id = #{userId}")
    Double getAverageRating(@Param("userId") Long userId);

    /**
     * 统计用户收到的评价数量
     */
    @Select("SELECT COUNT(*) FROM review_record WHERE reviewed_id = #{userId}")
    Long countReviewsReceived(@Param("userId") Long userId);

    /**
     * 统计用户收到的好评数量（4-5星）
     */
    @Select("SELECT COUNT(*) FROM review_record WHERE reviewed_id = #{userId} AND rating >= 4")
    Long countGoodReviews(@Param("userId") Long userId);
}
