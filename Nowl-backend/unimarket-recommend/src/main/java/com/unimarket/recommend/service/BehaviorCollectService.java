package com.unimarket.recommend.service;

/**
 * 用户行为收集服务接口
 */
public interface BehaviorCollectService {

    /**
     * 记录浏览行为
     * @param userId 用户ID
     * @param productId 商品ID
     * @param categoryId 分类ID
     * @param duration 浏览时长（秒）
     */
    void recordView(Long userId, Long productId, Integer categoryId, Integer duration);

    /**
     * 记录收藏行为
     * @param userId 用户ID
     * @param productId 商品ID
     * @param categoryId 分类ID
     */
    void recordCollect(Long userId, Long productId, Integer categoryId);

    /**
     * 记录购买行为
     * @param userId 用户ID
     * @param productId 商品ID
     * @param categoryId 分类ID
     */
    void recordBuy(Long userId, Long productId, Integer categoryId);

    /**
     * 记录搜索行为
     * @param userId 用户ID
     * @param keyword 搜索关键词
     */
    void recordSearch(Long userId, String keyword);

    /**
     * 更新商品热度分
     * @param productId 商品ID
     */
    void updateHotScore(Long productId);
}
