package com.unimarket.recommend.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.recommend.vo.RecommendItemVO;

import java.util.List;

/**
 * 推荐服务接口
 */
public interface RecommendService {

    /**
     * 首页推荐（猜你喜欢）
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 推荐结果
     */
    PageResult<RecommendItemVO> homeRecommend(Long userId, int pageNum, int pageSize,
                                              String schoolCode, String campusCode);

    /**
     * 热门推荐
     * @param categoryId 分类ID（可选）
     * @param size 数量
     * @return 热门商品
     */
    List<RecommendItemVO> hotRecommend(Long userId, Integer categoryId, int size,
                                       String schoolCode, String campusCode);

    /**
     * 相似商品推荐
     * @param productId 商品ID
     * @param size 数量
     * @return 相似商品
     */
    List<RecommendItemVO> similarRecommend(Long userId, Long productId, int size,
                                           String schoolCode, String campusCode);

    /**
     * 关注的人在卖
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 关注用户发布的商品
     */
    PageResult<RecommendItemVO> followingRecommend(Long userId, int pageNum, int pageSize,
                                                   String schoolCode, String campusCode);
}
