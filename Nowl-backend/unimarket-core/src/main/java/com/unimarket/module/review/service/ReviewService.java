package com.unimarket.module.review.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.module.review.dto.ReviewCreateDTO;
import com.unimarket.module.review.vo.ReviewListItemVO;
import com.unimarket.module.review.vo.UserReviewStatsVO;

/**
 * 评价服务接口
 */
public interface ReviewService {

    /**
     * 创建评价
     * @param userId 评价人ID
     * @param dto 评价DTO
     */
    void createReview(Long userId, ReviewCreateDTO dto);

    /**
     * 获取用户收到的评价列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 评价列表
     */
    PageResult<ReviewListItemVO> getReceivedReviews(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户发出的评价列表
     */
    PageResult<ReviewListItemVO> getSentReviews(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 获取用户评价统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    UserReviewStatsVO getUserReviewStats(Long userId);

    /**
     * 检查是否可以评价
     * @param userId 评价人ID
     * @param targetType 类型：0-订单，1-跑腿
     * @param contentId 内容ID
     * @param reviewedId 被评价人ID
     * @return 是否可评价
     */
    boolean canReview(Long userId, Integer targetType, Long contentId, Long reviewedId);

    /**
     * 检查订单是否已被某方评价
     */
    boolean hasReviewed(Long orderId, Long taskId, Integer targetType, Long reviewerId);
}
