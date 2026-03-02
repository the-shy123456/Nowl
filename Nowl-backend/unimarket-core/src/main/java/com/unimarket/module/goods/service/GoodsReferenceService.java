package com.unimarket.module.goods.service;

/**
 * 商品参考数据服务
 */
public interface GoodsReferenceService {

    /**
     * 构建估价参考数据
     *
     * @param categoryId 分类ID
     * @param limit 条数限制
     * @return 参考数据文本
     */
    String buildPriceReferenceData(Long categoryId, int limit);

    /**
     * 统计同类有效商品数量
     *
     * @param categoryId 分类ID
     * @return 商品数量
     */
    int countPriceReferenceGoods(Long categoryId);
}
