package com.unimarket.search.service;

import java.util.List;

/**
 * 搜索数据同步服务接口
 * 由 MQ 消费者调用
 */
public interface SearchSyncService {

    /**
     * 同步单个商品到ES
     * @param productId 商品ID
     */
    void syncGoods(Long productId);

    /**
     * 批量同步商品到ES
     * @param productIds 商品ID列表
     */
    void syncGoodsBatch(List<Long> productIds);

    /**
     * 从ES删除商品
     * @param productId 商品ID
     */
    void deleteGoods(Long productId);

    /**
     * 更新商品热度分
     * @param productId 商品ID
     * @param hotScore 热度分
     */
    void updateHotScore(Long productId, Double hotScore);

    /**
     * 更新商品浏览量
     * @param productId 商品ID
     * @param viewCount 浏览量
     */
    void updateViewCount(Long productId, Integer viewCount);

    /**
     * 全量同步所有商品
     */
    void fullSync();

    /**
     * 创建或更新索引
     */
    void createOrUpdateIndex();
}
