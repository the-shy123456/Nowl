package com.unimarket.recommend.algorithm;

import com.unimarket.recommend.vo.RecommendItemVO;

import java.util.List;

/**
 * 推荐算法接口
 */
public interface RecommendAlgorithm {

    /**
     * 为用户推荐商品
     * @param userId 用户ID
     * @param size 推荐数量
     * @param excludeProductIds 排除的商品ID列表
     * @return 推荐结果
     */
    List<RecommendItemVO> recommend(Long userId, int size, List<Long> excludeProductIds);

    /**
     * 获取相似商品
     * @param productId 商品ID
     * @param size 推荐数量
     * @return 相似商品列表
     */
    List<RecommendItemVO> getSimilarItems(Long productId, int size);

    /**
     * 获取算法名称
     */
    String getAlgorithmName();

    /**
     * 获取算法权重
     */
    double getWeight();
}
