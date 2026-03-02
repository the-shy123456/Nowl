package com.unimarket.module.goods.service;

import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商品权限校验服务
 * 用于 @PreAuthorize 注解中的权限判断
 *
 * 使用方式：@PreAuthorize("@goodsPermission.isOwner(#id)")
 */
@Slf4j
@Service("goodsPermission")
@RequiredArgsConstructor
public class GoodsPermissionService {

    private final GoodsInfoMapper goodsInfoMapper;

    /**
     * 判断当前用户是否为商品发布者
     */
    public boolean isOwner(Long productId, Long userId) {
        if (productId == null || userId == null) {
            return false;
        }
        GoodsInfo goods = goodsInfoMapper.selectById(productId);
        if (goods == null) {
            log.warn("商品不存在: productId={}", productId);
            return false;
        }
        return goods.getSellerId().equals(userId);
    }

    /**
     * 判断当前用户是否不是商品发布者（用于下单场景，不能购买自己的商品）
     */
    public boolean isNotOwner(Long productId, Long userId) {
        if (productId == null || userId == null) {
            return false;
        }
        GoodsInfo goods = goodsInfoMapper.selectById(productId);
        if (goods == null) {
            return false;
        }
        return !goods.getSellerId().equals(userId);
    }
}
