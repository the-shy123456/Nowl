package com.unimarket.recommend.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.recommend.mapper.UserBehaviorLogMapper;
import com.unimarket.recommend.vo.RecommendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 混合推荐器
 * 融合物品协同过滤和基于内容的推荐结果
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridRecommender {

    private final ItemCFAlgorithm itemCFAlgorithm;
    private final ContentBasedAlgorithm contentBasedAlgorithm;
    private final UserBehaviorLogMapper behaviorLogMapper;
    private final GoodsInfoMapper goodsInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;

    // 权重配置
    private static final double CF_WEIGHT = 0.5;      // 协同过滤权重
    private static final double CONTENT_WEIGHT = 0.3; // 内容推荐权重
    private static final double HOT_WEIGHT = 0.2;     // 热度权重

    // 缓存配置
    private static final String RECOMMEND_CACHE_KEY = "recommend:user:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    private static final int MAX_RECOMMEND_SIZE = 50;

    /**
     * 获取首页推荐（融合推荐）
     */
    public List<RecommendItemVO> recommend(Long userId, int size) {
        return recommend(userId, size, null);
    }

    /**
     * 获取推荐商品
     * @param userId 用户ID
     * @param size 推荐数量
     * @param excludeProductIds 排除的商品ID
     */
    public List<RecommendItemVO> recommend(Long userId, int size, List<Long> excludeProductIds) {
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        // 新用户冷启动处理
        if (userId == null || isNewUser(userId)) {
            log.info("新用户冷启动，使用热门推荐: userId={}", userId);
            return getHotRecommend(safeSize, excludeProductIds);
        }

        // 1. 获取协同过滤推荐
        List<RecommendItemVO> cfItems = itemCFAlgorithm.recommend(userId, safeSize * 2, excludeProductIds);
        log.debug("协同过滤推荐数量: {}", cfItems.size());

        // 2. 获取内容推荐
        List<RecommendItemVO> contentItems = contentBasedAlgorithm.recommend(userId, safeSize * 2, excludeProductIds);
        log.debug("内容推荐数量: {}", contentItems.size());

        // 3. 获取热门推荐（兜底）
        List<RecommendItemVO> hotItems = getHotRecommend(safeSize, excludeProductIds);

        // 4. 融合推荐结果
        List<RecommendItemVO> mergedItems = mergeAndRank(cfItems, contentItems, hotItems, safeSize);

        // 5. 后处理：去重、过滤
        return postProcess(mergedItems, userId, excludeProductIds, safeSize);
    }

    /**
     * 获取相似商品推荐（商品详情页）
     */
    public List<RecommendItemVO> getSimilarItems(Long productId, int size) {
        if (productId == null) {
            return Collections.emptyList();
        }
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);

        // 1. 获取协同过滤相似
        List<RecommendItemVO> cfSimilar = itemCFAlgorithm.getSimilarItems(productId, safeSize);

        // 2. 获取内容相似
        List<RecommendItemVO> contentSimilar = contentBasedAlgorithm.getSimilarItems(productId, safeSize);

        // 3. 融合
        return mergeAndRank(cfSimilar, contentSimilar, null, safeSize);
    }

    /**
     * 判断是否为新用户（行为数据少）
     */
    private boolean isNewUser(Long userId) {
        List<Long> products = behaviorLogMapper.selectRecentProductIds(userId, 5);
        return products.size() < 3;
    }

    /**
     * 获取热门商品推荐
     */
    public List<RecommendItemVO> getHotRecommend(int size, List<Long> excludeProductIds) {
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        // 从行为日志统计热门商品
        List<Map<String, Object>> hotProducts = behaviorLogMapper.selectHotProducts(safeSize * 2);

        List<Long> hotProductIds = hotProducts.stream()
            .map(m -> ((Number) m.get("product_id")).longValue())
            .filter(id -> excludeProductIds == null || !excludeProductIds.contains(id))
            .limit(safeSize)
            .collect(Collectors.toList());

        if (hotProductIds.isEmpty()) {
            // 如果没有热门数据，按收藏数排序获取
            return getFallbackRecommend(safeSize, excludeProductIds);
        }

        // 查询商品信息
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GoodsInfo::getProductId, hotProductIds)
               .eq(GoodsInfo::getTradeStatus, 0)
               .in(GoodsInfo::getReviewStatus, 1, 2);

        List<GoodsInfo> goodsList = goodsInfoMapper.selectList(wrapper);

        Map<Long, Double> scoreMap = new HashMap<>();
        for (Map<String, Object> m : hotProducts) {
            Long productId = ((Number) m.get("product_id")).longValue();
            Double score = ((Number) m.get("hot_score")).doubleValue();
            scoreMap.put(productId, score);
        }

        double maxScore = scoreMap.values().stream().max(Double::compare).orElse(1.0);

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
            vo.setScore(scoreMap.getOrDefault(goods.getProductId(), 0.0) / maxScore);
            vo.setRecommendType("hot");
            return vo;
        }).sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
          .limit(safeSize)
          .collect(Collectors.toList());
    }

    /**
     * 兜底推荐（按收藏数）
     */
    private List<RecommendItemVO> getFallbackRecommend(int size, List<Long> excludeProductIds) {
        int safeSize = clamp(size, 1, MAX_RECOMMEND_SIZE);
        LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoodsInfo::getTradeStatus, 0)
               .in(GoodsInfo::getReviewStatus, 1, 2)
               .orderByDesc(GoodsInfo::getCollectCount);

        if (excludeProductIds != null && !excludeProductIds.isEmpty()) {
            wrapper.notIn(GoodsInfo::getProductId, excludeProductIds);
        }

        Page<GoodsInfo> page = goodsInfoMapper.selectPage(new Page<>(1, safeSize), wrapper);
        List<GoodsInfo> goodsList = page.getRecords();

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
            vo.setScore(0.5);
            vo.setRecommendType("fallback");
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 融合多路推荐结果并排序
     */
    private List<RecommendItemVO> mergeAndRank(
            List<RecommendItemVO> cfItems,
            List<RecommendItemVO> contentItems,
            List<RecommendItemVO> hotItems,
            int size) {

        Map<Long, RecommendItemVO> merged = new HashMap<>();
        Map<Long, Double> finalScores = new HashMap<>();

        // 融合协同过滤结果
        if (cfItems != null) {
            double maxCfScore = cfItems.stream().mapToDouble(item -> safeScore(item.getScore())).max().orElse(1.0);
            for (RecommendItemVO item : cfItems) {
                merged.put(item.getProductId(), item);
                double normalizedScore = safeScore(item.getScore()) / Math.max(maxCfScore, 0.001);
                finalScores.merge(item.getProductId(), normalizedScore * CF_WEIGHT, Double::sum);
            }
        }

        // 融合内容推荐结果
        if (contentItems != null) {
            double maxContentScore = contentItems.stream().mapToDouble(item -> safeScore(item.getScore())).max().orElse(1.0);
            for (RecommendItemVO item : contentItems) {
                merged.putIfAbsent(item.getProductId(), item);
                double normalizedScore = safeScore(item.getScore()) / Math.max(maxContentScore, 0.001);
                finalScores.merge(item.getProductId(), normalizedScore * CONTENT_WEIGHT, Double::sum);
            }
        }

        // 融合热门推荐结果
        if (hotItems != null) {
            double maxHotScore = hotItems.stream().mapToDouble(item -> safeScore(item.getScore())).max().orElse(1.0);
            for (RecommendItemVO item : hotItems) {
                merged.putIfAbsent(item.getProductId(), item);
                double normalizedScore = safeScore(item.getScore()) / Math.max(maxHotScore, 0.001);
                finalScores.merge(item.getProductId(), normalizedScore * HOT_WEIGHT, Double::sum);
            }
        }

        // 更新最终分数并排序
        return finalScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(size)
            .map(entry -> {
                RecommendItemVO item = merged.get(entry.getKey());
                item.setScore(entry.getValue());
                item.setRecommendType("hybrid");
                return item;
            })
            .collect(Collectors.toList());
    }


    private double safeScore(Double score) {
        return score == null ? 0.0 : score;
    }

    /**
     * 后处理：去重、过滤已购买/已下架
     */
    private List<RecommendItemVO> postProcess(
            List<RecommendItemVO> items,
            Long userId,
            List<Long> excludeProductIds,
            int size) {

        Set<Long> seen = new HashSet<>();
        if (excludeProductIds != null) {
            seen.addAll(excludeProductIds);
        }

        return items.stream()
            .filter(item -> {
                if (seen.contains(item.getProductId())) {
                    return false;
                }
                seen.add(item.getProductId());
                return true;
            })
            .limit(size)
            .collect(Collectors.toList());
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
