package com.unimarket.module.goods.service;

import com.unimarket.common.result.PageResult;
import com.unimarket.module.goods.dto.GoodsPublishDTO;
import com.unimarket.module.goods.dto.GoodsQueryDTO;
import com.unimarket.module.goods.vo.GoodsDetailVO;
import com.unimarket.module.goods.vo.GoodsVO;

/**
 * 商品Service接口
 */
public interface GoodsService {

    /**
     * 查询商品列表
     */
    PageResult<GoodsVO> list(GoodsQueryDTO dto, Long currentUserId);

    /**
     * 查询商品详情
     */
    GoodsDetailVO getDetail(Long productId, Long currentUserId);

    /**
     * 发布商品
     */
    void publish(Long userId, GoodsPublishDTO dto);

    /**
     * 更新商品
     */
    void update(Long userId, Long productId, GoodsPublishDTO dto);

    /**
     * 删除商品
     */
    void delete(Long userId, Long productId);

    /**
     * 下架商品
     */
    void offshelf(Long userId, Long productId);

    /**
     * 收藏商品
     */
    void collect(Long userId, Long productId);

    /**
     * 取消收藏
     */
    void uncollect(Long userId, Long productId);

    /**
     * 查询我的商品
     */
    PageResult<GoodsVO> getMyGoods(Long userId, GoodsQueryDTO dto);

    /**
     * 查询我的收藏
     */
    PageResult<GoodsVO> getMyCollections(Long userId, GoodsQueryDTO dto);
}
