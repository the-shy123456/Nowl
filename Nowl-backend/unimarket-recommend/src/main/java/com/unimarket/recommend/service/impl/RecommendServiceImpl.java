package com.unimarket.recommend.service.impl;

import com.unimarket.common.enums.ReviewStatus;
import com.unimarket.common.enums.TradeStatus;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.common.result.PageResult;
import com.unimarket.module.goods.entity.CollectionRecord;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.CollectionRecordMapper;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.module.user.entity.UserFollow;
import com.unimarket.module.user.mapper.UserFollowMapper;
import com.unimarket.recommend.algorithm.HybridRecommender;
import com.unimarket.recommend.service.RecommendService;
import com.unimarket.recommend.vo.RecommendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 推荐服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final HybridRecommender hybridRecommender;
    private final GoodsInfoMapper goodsInfoMapper;
    private final CollectionRecordMapper collectionRecordMapper;
    private final UserFollowMapper userFollowMapper;

    private static final int FILTER_BOOST = 3;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_RECOMMEND_SIZE = 50;

    @Override
    public PageResult<RecommendItemVO> homeRecommend(Long userId, int pageNum, int pageSize,
                                                     String schoolCode, String campusCode) {
        int safePageNum = clamp(pageNum, 1, Integer.MAX_VALUE);
        int safePageSize = clamp(pageSize, 1, MAX_PAGE_SIZE);
        int offset = (safePageNum - 1) * safePageSize;
        int totalNeed = offset + safePageSize;
        boolean hasCampusFilter = hasSchoolOrCampus(schoolCode, campusCode);
        if (hasCampusFilter) {
            totalNeed *= FILTER_BOOST;
        }

        List<RecommendItemVO> allItems;
        try {
            allItems = hybridRecommender.recommend(userId, totalNeed, null);
        } catch (Exception ex) {
            log.error("首页推荐异常，降级到按发布时间推荐: userId={}", userId, ex);
            return fallbackHomeRecommend(userId, safePageNum, safePageSize, schoolCode, campusCode);
        }

        if (allItems == null || allItems.isEmpty()) {
            return fallbackHomeRecommend(userId, safePageNum, safePageSize, schoolCode, campusCode);
        }

        List<RecommendItemVO> filteredItems = filterItemsBySchoolCampus(allItems, schoolCode, campusCode);
        if (filteredItems.isEmpty()) {
            return fallbackHomeRecommend(userId, safePageNum, safePageSize, schoolCode, campusCode);
        }

        int start = Math.min(offset, filteredItems.size());
        int end = Math.min(offset + safePageSize, filteredItems.size());
        if (start >= end) {
            return new PageResult<>(Long.valueOf(filteredItems.size()), new ArrayList<RecommendItemVO>());
        }

        List<RecommendItemVO> pageItems = new ArrayList<>(filteredItems.subList(start, end));
        fillCollectedStatus(pageItems, userId);

        long total = Math.max(filteredItems.size(), pageNum * pageSize);
        if (pageItems.size() < safePageSize) {
            total = Math.max(filteredItems.size(), offset + pageItems.size());
        }

        return new PageResult<>(total, pageItems);
    }

    @Override
    public List<RecommendItemVO> hotRecommend(Long userId, Integer categoryId, int size,
                                              String schoolCode, String campusCode) {
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsInfo::getTradeStatus, TradeStatus.ON_SALE.getCode())
               .in(GoodsInfo::getReviewStatus, ReviewStatus.AI_PASSED.getCode(), ReviewStatus.MANUAL_PASSED.getCode());

        if (categoryId != null) {
            wrapper.eq(GoodsInfo::getCategoryId, categoryId);
        }
        if (StrUtil.isNotBlank(schoolCode)) {
            wrapper.eq(GoodsInfo::getSchoolCode, schoolCode);
        }
        if (StrUtil.isNotBlank(campusCode)) {
            wrapper.eq(GoodsInfo::getCampusCode, campusCode);
        }

        wrapper.orderByDesc(GoodsInfo::getCollectCount);
        Page<GoodsInfo> page = goodsInfoMapper.selectPage(new Page<>(1, safeSize), wrapper);
        List<GoodsInfo> goodsList = page.getRecords();

        List<RecommendItemVO> items = goodsList.stream().map(goods -> {
            RecommendItemVO vo = new RecommendItemVO();
            vo.setProductId(goods.getProductId());
            vo.setTitle(goods.getTitle());
            vo.setImage(goods.getImage());
            vo.setPrice(goods.getPrice());
            vo.setOriginalPrice(goods.getOriginalPrice());
            vo.setCategoryId(goods.getCategoryId());
            vo.setSellerId(goods.getSellerId());
            vo.setCollectCount(goods.getCollectCount());
            vo.setRecommendType("hot");
            return vo;
        }).collect(Collectors.toList());

        fillCollectedStatus(items, userId);
        return items;
    }

    @Override
    public List<RecommendItemVO> similarRecommend(Long userId, Long productId, int size,
                                                  String schoolCode, String campusCode) {
        if (productId == null) {
            return Collections.emptyList();
        }
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        boolean hasCampusFilter = hasSchoolOrCampus(schoolCode, campusCode);
        int requestSize = hasCampusFilter ? safeSize * FILTER_BOOST : safeSize;
        List<RecommendItemVO> items = hybridRecommender.getSimilarItems(productId, requestSize);
        items = filterItemsBySchoolCampus(items, schoolCode, campusCode);
        List<RecommendItemVO> pageItems = items.size() > safeSize ? items.subList(0, safeSize) : items;
        fillCollectedStatus(pageItems, userId);
        return pageItems;
    }

    private PageResult<RecommendItemVO> fallbackHomeRecommend(Long userId, int pageNum, int pageSize,
                                                           String schoolCode, String campusCode) {
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsInfo::getTradeStatus, TradeStatus.ON_SALE.getCode())
                .in(GoodsInfo::getReviewStatus, ReviewStatus.AI_PASSED.getCode(), ReviewStatus.MANUAL_PASSED.getCode())
                .orderByDesc(GoodsInfo::getCreateTime);

        if (StrUtil.isNotBlank(schoolCode)) {
            wrapper.eq(GoodsInfo::getSchoolCode, schoolCode);
        }
        if (StrUtil.isNotBlank(campusCode)) {
            wrapper.eq(GoodsInfo::getCampusCode, campusCode);
        }

        Page<GoodsInfo> page = new Page<>(pageNum, pageSize);
        Page<GoodsInfo> result = goodsInfoMapper.selectPage(page, wrapper);

        List<RecommendItemVO> items = result.getRecords().stream().map(goods -> {
            RecommendItemVO vo = new RecommendItemVO();
            vo.setProductId(goods.getProductId());
            vo.setTitle(goods.getTitle());
            vo.setImage(goods.getImage());
            vo.setPrice(goods.getPrice());
            vo.setOriginalPrice(goods.getOriginalPrice());
            vo.setCategoryId(goods.getCategoryId());
            vo.setSellerId(goods.getSellerId());
            vo.setCollectCount(goods.getCollectCount());
            vo.setRecommendType("fallback");
            return vo;
        }).collect(Collectors.toList());

        fillCollectedStatus(items, userId);
        return new PageResult<>(result.getTotal(), items);
    }

    @Override
    public PageResult<RecommendItemVO> followingRecommend(Long userId, int pageNum, int pageSize,
                                                          String schoolCode, String campusCode) {
        int safePageNum = clamp(pageNum, 1, Integer.MAX_VALUE);
        int safePageSize = clamp(pageSize, 1, MAX_PAGE_SIZE);
        if (userId == null) {
            return new PageResult<>(0L, new ArrayList<>());
        }

        // 获取关注的用户列表
        LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
        followWrapper.eq(UserFollow::getUserId, userId);
        List<UserFollow> follows = userFollowMapper.selectList(followWrapper);

        if (follows.isEmpty()) {
            return new PageResult<>(0L, new ArrayList<>());
        }

        List<Long> followedUserIds = follows.stream()
            .map(UserFollow::getFollowedUserId)
            .collect(Collectors.toList());

        // 查询关注用户发布的商品
        LambdaQueryWrapper<GoodsInfo> goodsWrapper = new LambdaQueryWrapper<>();
        goodsWrapper.in(GoodsInfo::getSellerId, followedUserIds)
                    .eq(GoodsInfo::getTradeStatus, TradeStatus.ON_SALE.getCode())
                    .in(GoodsInfo::getReviewStatus, ReviewStatus.AI_PASSED.getCode(), ReviewStatus.MANUAL_PASSED.getCode())
                    .orderByDesc(GoodsInfo::getCreateTime);
        if (StrUtil.isNotBlank(schoolCode)) {
            goodsWrapper.eq(GoodsInfo::getSchoolCode, schoolCode);
        }
        if (StrUtil.isNotBlank(campusCode)) {
            goodsWrapper.eq(GoodsInfo::getCampusCode, campusCode);
        }

        Page<GoodsInfo> page = new Page<>(safePageNum, safePageSize);
        Page<GoodsInfo> result = goodsInfoMapper.selectPage(page, goodsWrapper);

        List<RecommendItemVO> items = result.getRecords().stream().map(goods -> {
            RecommendItemVO vo = new RecommendItemVO();
            vo.setProductId(goods.getProductId());
            vo.setTitle(goods.getTitle());
            vo.setImage(goods.getImage());
            vo.setPrice(goods.getPrice());
            vo.setCategoryId(goods.getCategoryId());
            vo.setSellerId(goods.getSellerId());
            vo.setCollectCount(goods.getCollectCount());
            vo.setRecommendType("following");
            return vo;
        }).collect(Collectors.toList());

        fillCollectedStatus(items, userId);
        return new PageResult<>(result.getTotal(), items);
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    private void fillCollectedStatus(List<RecommendItemVO> items, Long userId) {
        if (userId == null || items == null || items.isEmpty()) {
            return;
        }
        List<Long> productIds = items.stream()
            .map(RecommendItemVO::getProductId)
            .distinct()
            .collect(Collectors.toList());
        if (productIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<CollectionRecord> collectionWrapper = new LambdaQueryWrapper<>();
        collectionWrapper.eq(CollectionRecord::getUserId, userId)
            .in(CollectionRecord::getProductId, productIds);
        List<CollectionRecord> collections = collectionRecordMapper.selectList(collectionWrapper);
        if (collections.isEmpty()) {
            return;
        }
        java.util.Set<Long> collectedIds = collections.stream()
            .map(CollectionRecord::getProductId)
            .collect(Collectors.toSet());
        for (RecommendItemVO item : items) {
            if (item.getProductId() != null) {
                item.setIsCollected(collectedIds.contains(item.getProductId()));
            }
        }
    }

    private boolean hasSchoolOrCampus(String schoolCode, String campusCode) {
        return StrUtil.isNotBlank(schoolCode) || StrUtil.isNotBlank(campusCode);
    }

    private List<RecommendItemVO> filterItemsBySchoolCampus(List<RecommendItemVO> items,
                                                            String schoolCode,
                                                            String campusCode) {
        if (items.isEmpty() || !hasSchoolOrCampus(schoolCode, campusCode)) {
            return items;
        }

        List<Long> productIds = items.stream()
            .map(RecommendItemVO::getProductId)
            .distinct()
            .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return items;
        }

        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GoodsInfo::getProductId, productIds)
               .eq(GoodsInfo::getTradeStatus, TradeStatus.ON_SALE.getCode())
               .in(GoodsInfo::getReviewStatus, ReviewStatus.AI_PASSED.getCode(), ReviewStatus.MANUAL_PASSED.getCode());

        if (StrUtil.isNotBlank(schoolCode)) {
            wrapper.eq(GoodsInfo::getSchoolCode, schoolCode);
        }
        if (StrUtil.isNotBlank(campusCode)) {
            wrapper.eq(GoodsInfo::getCampusCode, campusCode);
        }

        List<GoodsInfo> allowedGoods = goodsInfoMapper.selectList(wrapper);
        if (allowedGoods.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> allowedIds = allowedGoods.stream()
            .map(GoodsInfo::getProductId)
            .collect(Collectors.toList());

        return items.stream()
            .filter(item -> allowedIds.contains(item.getProductId()))
            .collect(Collectors.toList());
    }
}
