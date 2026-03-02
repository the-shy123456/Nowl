package com.unimarket.module.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.constant.CacheConstants;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.goods.entity.ItemCategory;
import com.unimarket.module.goods.mapper.ItemCategoryMapper;
import com.unimarket.module.goods.service.CategoryService;
import com.unimarket.module.goods.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 分类Service实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final ItemCategoryMapper itemCategoryMapper;
    private final RedisCache redisCache;
    private final RedissonClient redissonClient;


    @Override
    public List<CategoryVO> getCategoryTree() {
        return getCachedCategoryTree().stream()
                .map(this::toCategoryTreeVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryVO> getFirstLevelCategories() {
        return getCachedCategoryTree().stream()
                .map(this::toCategorySummaryVO)
                .collect(Collectors.toList());
    }

    private List<ItemCategory> getCachedCategoryTree() {
        // 1. 先从缓存获取
        List<ItemCategory> cachedTree = redisCache.getCacheList(CacheConstants.GOODS_CATEGORY);
        if (cachedTree != null) {
            return cachedTree;
        }

        // 2. 加锁防止击穿
        String lockKey = "lock:category:tree";
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {
                // 双重检查
                cachedTree = redisCache.getCacheList(CacheConstants.GOODS_CATEGORY);
                if (cachedTree != null) {
                    return cachedTree;
                }

                // 3. 查询所有分类并构建树
                LambdaQueryWrapper<ItemCategory> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ItemCategory::getStatus, 1)
                        .orderByAsc(ItemCategory::getSort);
                List<ItemCategory> allCategories = itemCategoryMapper.selectList(wrapper);
                List<ItemCategory> tree = buildCategoryTree(allCategories);

                // 4. 写入缓存（使用随机 TTL 防雪崩）
                redisCache.setWithJitter(CacheConstants.GOODS_CATEGORY, tree, CacheConstants.GOODS_CATEGORY_EXPIRE, 10);
                return tree;
            } else {
                log.warn("未能获取分类树缓存锁，降级查询数据库");
                List<ItemCategory> allCategories = itemCategoryMapper.selectList(
                        new LambdaQueryWrapper<ItemCategory>()
                                .eq(ItemCategory::getStatus, 1)
                                .orderByAsc(ItemCategory::getSort)
                );
                return buildCategoryTree(allCategories);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("系统繁忙");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public List<Integer> getChildCategoryIds(Integer parentCategoryId) {
        if (parentCategoryId == null) {
            return List.of();
        }

        LambdaQueryWrapper<ItemCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItemCategory::getParentId, parentCategoryId)
                .eq(ItemCategory::getStatus, 1);

        return itemCategoryMapper.selectList(wrapper).stream()
                .map(ItemCategory::getCategoryId)
                .collect(Collectors.toList());
    }

    private CategoryVO toCategoryTreeVO(ItemCategory category) {
        CategoryVO vo = toCategorySummaryVO(category);
        List<ItemCategory> children = category.getChildren();
        if (children != null && !children.isEmpty()) {
            vo.setChildren(children.stream()
                    .map(this::toCategoryTreeVO)
                    .collect(Collectors.toList()));
        }
        return vo;
    }

    private CategoryVO toCategorySummaryVO(ItemCategory category) {
        CategoryVO vo = new CategoryVO();
        vo.setCategoryId(category.getCategoryId());
        vo.setCategoryName(category.getCategoryName());
        vo.setParentId(category.getParentId());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        return vo;
    }

    /**
     * 构建分类树
     */
    private List<ItemCategory> buildCategoryTree(List<ItemCategory> allCategories) {
        // 按父ID分组
        Map<Integer, List<ItemCategory>> categoryMap = allCategories.stream()
                .collect(Collectors.groupingBy(ItemCategory::getParentId));

        // 获取一级分类
        List<ItemCategory> rootCategories = categoryMap.getOrDefault(0, new ArrayList<>());

        // 递归构建树
        for (ItemCategory category : rootCategories) {
            buildChildren(category, categoryMap);
        }

        return rootCategories;
    }

    /**
     * 递归构建子分类
     */
    private void buildChildren(ItemCategory parent, Map<Integer, List<ItemCategory>> categoryMap) {
        List<ItemCategory> children = categoryMap.get(parent.getCategoryId());
        if (children != null && !children.isEmpty()) {
            parent.setChildren(children);
            for (ItemCategory child : children) {
                buildChildren(child, categoryMap);
            }
        }
    }
}
