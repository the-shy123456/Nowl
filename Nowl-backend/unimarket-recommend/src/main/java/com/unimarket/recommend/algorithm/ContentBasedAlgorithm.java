package com.unimarket.recommend.algorithm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.recommend.entity.GoodsSimilarity;
import com.unimarket.recommend.entity.UserPreference;
import com.unimarket.recommend.mapper.GoodsSimilarityMapper;
import com.unimarket.recommend.mapper.UserBehaviorLogMapper;
import com.unimarket.recommend.mapper.UserPreferenceMapper;
import com.unimarket.recommend.vo.RecommendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于内容的推荐算法
 * 根据商品属性（分类、价格等）计算相似度，推荐相似商品
 *
 * 核心思想：
 * 1. 分析用户历史偏好的商品特征（分类、价格区间）
 * 2. 找出具有相似特征的其他商品推荐给用户
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentBasedAlgorithm implements RecommendAlgorithm {

    private final UserPreferenceMapper preferenceMapper;
    private final UserBehaviorLogMapper behaviorLogMapper;
    private final GoodsSimilarityMapper similarityMapper;
    private final GoodsInfoMapper goodsInfoMapper;

    private static final double ALGORITHM_WEIGHT = 0.3;

    // 内容相似度权重
    private static final double CATEGORY_WEIGHT = 0.5;  // 分类权重
    private static final double PRICE_WEIGHT = 0.3;     // 价格权重
    private static final double SCHOOL_WEIGHT = 0.2;    // 学校权重
    private static final int MAX_RECOMMEND_SIZE = 50;

    @Override
    public List<RecommendItemVO> recommend(Long userId, int size, List<Long> excludeProductIds) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);

        // 1. 获取用户偏好
        UserPreference preference = preferenceMapper.selectById(userId);
        Map<Integer, Double> categoryScores = new HashMap<>();

        if (preference != null && preference.getCategoryScores() != null) {
            categoryScores = JSON.parseObject(
                preference.getCategoryScores(),
                new TypeReference<Map<Integer, Double>>() {}
            );
        }

        // 如果没有偏好数据，从行为日志实时计算
        if (categoryScores.isEmpty()) {
            List<Map<String, Object>> categoryPrefs = behaviorLogMapper.selectCategoryPreference(userId);
            for (Map<String, Object> pref : categoryPrefs) {
                Integer categoryId = (Integer) pref.get("category_id");
                Number score = (Number) pref.get("score");
                if (categoryId != null && score != null) {
                    categoryScores.put(categoryId, score.doubleValue());
                }
            }
        }

        if (categoryScores.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 根据用户偏好的分类查询商品
        List<Integer> preferredCategories = categoryScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 获取用户已交互的商品
        List<Long> userProducts = behaviorLogMapper.selectRecentProductIds(userId, 100);

        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GoodsInfo::getCategoryId, preferredCategories)
               .eq(GoodsInfo::getTradeStatus, 0)
               .in(GoodsInfo::getReviewStatus, 1, 2)
               .notIn(userProducts != null && !userProducts.isEmpty(), GoodsInfo::getProductId, userProducts)
               .orderByDesc(GoodsInfo::getCollectCount);

        if (excludeProductIds != null && !excludeProductIds.isEmpty()) {
            wrapper.notIn(GoodsInfo::getProductId, excludeProductIds);
        }

        Page<GoodsInfo> candidatesPage = goodsInfoMapper.selectPage(new Page<>(1, safeSize * 3L), wrapper);
        List<GoodsInfo> candidates = candidatesPage.getRecords();

        // 3. 计算每个候选商品的推荐分数
        Map<Integer, Double> finalCategoryScores = categoryScores;
        double maxCategoryScore = categoryScores.values().stream().max(Double::compare).orElse(1.0);

        List<RecommendItemVO> results = candidates.stream().map(goods -> {
            RecommendItemVO vo = new RecommendItemVO();
            vo.setProductId(goods.getProductId());
            vo.setTitle(goods.getTitle());
            vo.setImage(goods.getImage());
            vo.setPrice(goods.getPrice());
            vo.setOriginalPrice(goods.getOriginalPrice());
            vo.setCategoryId(goods.getCategoryId());
            vo.setSellerId(goods.getSellerId());
            vo.setCollectCount(goods.getCollectCount());
            vo.setRecommendType("content");

            // 计算分数 = 分类偏好分 + 收藏数加成
            double categoryScore = finalCategoryScores.getOrDefault(goods.getCategoryId(), 0.0) / maxCategoryScore;
            double popularityScore = Math.log1p(goods.getCollectCount() != null ? goods.getCollectCount() : 0) / 10;
            vo.setScore(categoryScore * 0.7 + popularityScore * 0.3);

            return vo;
        }).sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
          .limit(safeSize)
          .collect(Collectors.toList());

        return results;
    }

    @Override
    public List<RecommendItemVO> getSimilarItems(Long productId, int size) {
        if (productId == null) {
            return Collections.emptyList();
        }
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);

        // 查询商品信息
        GoodsInfo targetGoods = goodsInfoMapper.selectById(productId);
        if (targetGoods == null) {
            return Collections.emptyList();
        }

        // 1. 优先从预计算的相似度表获取
        List<GoodsSimilarity> precomputed = similarityMapper.selectSimilarProducts(
            productId,
            GoodsSimilarity.SimilarityType.CONTENT_BASED,
            safeSize
        );

        if (!precomputed.isEmpty()) {
            List<Long> productIds = precomputed.stream()
                .map(GoodsSimilarity::getSimilarProductId)
                .collect(Collectors.toList());

            Map<Long, Double> scoreMap = precomputed.stream()
                .collect(Collectors.toMap(
                    GoodsSimilarity::getSimilarProductId,
                    s -> s.getSimilarityScore().doubleValue()
                ));

            return getRecommendItems(productIds, scoreMap);
        }

        // 2. 实时计算相似商品
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsInfo::getCategoryId, targetGoods.getCategoryId())
               .eq(GoodsInfo::getTradeStatus, 0)
               .in(GoodsInfo::getReviewStatus, 1, 2)
               .ne(GoodsInfo::getProductId, productId);

        Page<GoodsInfo> candidatesPage = goodsInfoMapper.selectPage(new Page<>(1, safeSize * 3L), wrapper);
        List<GoodsInfo> candidates = candidatesPage.getRecords();

        // 计算相似度分数
        return candidates.stream().map(goods -> {
            RecommendItemVO vo = new RecommendItemVO();
            vo.setProductId(goods.getProductId());
            vo.setTitle(goods.getTitle());
            vo.setImage(goods.getImage());
            vo.setPrice(goods.getPrice());
            vo.setOriginalPrice(goods.getOriginalPrice());
            vo.setCategoryId(goods.getCategoryId());
            vo.setSellerId(goods.getSellerId());
            vo.setCollectCount(goods.getCollectCount());
            vo.setRecommendType("content");

            // 计算内容相似度
            double score = calculateContentSimilarity(targetGoods, goods).doubleValue();
            vo.setScore(score);

            return vo;
        }).sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
          .limit(safeSize)
          .collect(Collectors.toList());
    }

    /**
     * 计算两个商品的内容相似度
     */
    public BigDecimal calculateContentSimilarity(GoodsInfo goods1, GoodsInfo goods2) {
        double score = 0.0;

        // 1. 分类相似度（同分类为1，否则为0）
        if (goods1.getCategoryId().equals(goods2.getCategoryId())) {
            score += CATEGORY_WEIGHT;
        }

        // 2. 价格相似度（价格差异越小越相似）
        double price1 = goods1.getPrice().doubleValue();
        double price2 = goods2.getPrice().doubleValue();
        double priceDiff = Math.abs(price1 - price2) / Math.max(price1, price2);
        double priceSimilarity = Math.max(0, 1 - priceDiff);
        score += PRICE_WEIGHT * priceSimilarity;

        // 3. 学校相似度（同校为1，同城为0.5，否则为0）
        if (goods1.getSchoolCode() != null && goods1.getSchoolCode().equals(goods2.getSchoolCode())) {
            if (goods1.getCampusCode() != null && goods1.getCampusCode().equals(goods2.getCampusCode())) {
                score += SCHOOL_WEIGHT;
            } else {
                score += SCHOOL_WEIGHT * 0.7;
            }
        }

        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 获取推荐商品详情
     */
    private List<RecommendItemVO> getRecommendItems(List<Long> productIds, Map<Long, Double> scoreMap) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GoodsInfo::getProductId, productIds)
               .eq(GoodsInfo::getTradeStatus, 0)
               .in(GoodsInfo::getReviewStatus, 1, 2);

        List<GoodsInfo> goodsList = goodsInfoMapper.selectList(wrapper);

        return goodsList.stream().map(goods -> {
            RecommendItemVO vo = new RecommendItemVO();
            vo.setProductId(goods.getProductId());
            vo.setTitle(goods.getTitle());
            vo.setImage(goods.getImage());
            vo.setPrice(goods.getPrice());
            vo.setOriginalPrice(goods.getOriginalPrice());
            vo.setCategoryId(goods.getCategoryId());
            vo.setSellerId(goods.getSellerId());
            vo.setCollectCount(goods.getCollectCount());
            vo.setScore(scoreMap.getOrDefault(goods.getProductId(), 0.0));
            vo.setRecommendType("content");
            return vo;
        }).sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
          .collect(Collectors.toList());
    }

    @Override
    public String getAlgorithmName() {
        return "ContentBased";
    }

    @Override
    public double getWeight() {
        return ALGORITHM_WEIGHT;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
