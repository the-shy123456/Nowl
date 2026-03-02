package com.unimarket.recommend.algorithm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.recommend.entity.GoodsSimilarity;
import com.unimarket.recommend.entity.UserBehaviorLog;
import com.unimarket.recommend.mapper.GoodsSimilarityMapper;
import com.unimarket.recommend.mapper.UserBehaviorLogMapper;
import com.unimarket.recommend.vo.RecommendItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 物品协同过滤算法
 * 基于用户行为计算商品相似度，推荐用户可能感兴趣的商品
 *
 * 核心思想：
 * 1. 如果用户A和用户B都喜欢商品X，那么用户A喜欢的其他商品，用户B也可能喜欢
 * 2. 通过计算商品之间的共现关系来确定相似度
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemCFAlgorithm implements RecommendAlgorithm {

    private final UserBehaviorLogMapper behaviorLogMapper;
    private final GoodsSimilarityMapper similarityMapper;
    private final GoodsInfoMapper goodsInfoMapper;

    private static final double ALGORITHM_WEIGHT = 0.5;

    @Override
    public List<RecommendItemVO> recommend(Long userId, int size, List<Long> excludeProductIds) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 获取用户最近交互过的商品
        List<Long> userProducts = behaviorLogMapper.selectRecentProductIds(userId, 50);
        if (userProducts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取这些商品的相似商品
        Map<Long, Double> candidateScores = new HashMap<>();

        for (Long productId : userProducts) {
            List<GoodsSimilarity> similarItems = similarityMapper.selectSimilarProducts(
                productId,
                GoodsSimilarity.SimilarityType.COLLABORATIVE,
                20
            );

            for (GoodsSimilarity sim : similarItems) {
                Long similarProductId = sim.getSimilarProductId();

                // 排除用户已交互过的商品
                if (userProducts.contains(similarProductId)) {
                    continue;
                }

                // 排除指定的商品
                if (excludeProductIds != null && excludeProductIds.contains(similarProductId)) {
                    continue;
                }

                // 累加相似度分数
                double score = sim.getSimilarityScore().doubleValue();
                candidateScores.merge(similarProductId, score, Double::sum);
            }
        }

        if (candidateScores.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 按分数排序并取TopN
        List<Long> topProductIds = candidateScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(size)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        // 4. 查询商品信息并转换为VO
        return getRecommendItems(topProductIds, candidateScores);
    }

    @Override
    public List<RecommendItemVO> getSimilarItems(Long productId, int size) {
        if (productId == null) {
            return Collections.emptyList();
        }

        List<GoodsSimilarity> similarItems = similarityMapper.selectSimilarProducts(
            productId,
            GoodsSimilarity.SimilarityType.COLLABORATIVE,
            size
        );

        if (similarItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productIds = similarItems.stream()
            .map(GoodsSimilarity::getSimilarProductId)
            .collect(Collectors.toList());

        Map<Long, Double> scoreMap = similarItems.stream()
            .collect(Collectors.toMap(
                GoodsSimilarity::getSimilarProductId,
                s -> s.getSimilarityScore().doubleValue()
            ));

        return getRecommendItems(productIds, scoreMap);
    }

    /**
     * 计算两个商品的相似度（余弦相似度）
     */
    public BigDecimal calculateSimilarity(Long productId1, Long productId2) {
        // 获取共同用户数
        Integer commonUsers = behaviorLogMapper.selectCommonUserCount(productId1, productId2);
        if (commonUsers == null || commonUsers == 0) {
            return BigDecimal.ZERO;
        }

        // 获取各自的用户数
        Integer users1 = behaviorLogMapper.selectProductUserCount(productId1);
        Integer users2 = behaviorLogMapper.selectProductUserCount(productId2);

        if (users1 == null || users1 == 0 || users2 == null || users2 == 0) {
            return BigDecimal.ZERO;
        }

        // 余弦相似度 = 共同用户数 / sqrt(用户数1 * 用户数2)
        double similarity = commonUsers / Math.sqrt(users1 * users2);

        return BigDecimal.valueOf(similarity).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * 获取推荐商品详情
     */
    private List<RecommendItemVO> getRecommendItems(List<Long> productIds, Map<Long, Double> scoreMap) {
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 查询在售且已审核的商品
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
            vo.setRecommendType("cf"); // 协同过滤
            return vo;
        }).sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
          .collect(Collectors.toList());
    }

    @Override
    public String getAlgorithmName() {
        return "ItemCF";
    }

    @Override
    public double getWeight() {
        return ALGORITHM_WEIGHT;
    }
}
