package com.unimarket.recommend.job;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.recommend.algorithm.ContentBasedAlgorithm;
import com.unimarket.recommend.algorithm.ItemCFAlgorithm;
import com.unimarket.recommend.entity.GoodsSimilarity;
import com.unimarket.recommend.entity.UserBehaviorLog;
import com.unimarket.recommend.entity.UserPreference;
import com.unimarket.recommend.mapper.GoodsSimilarityMapper;
import com.unimarket.recommend.mapper.UserBehaviorLogMapper;
import com.unimarket.recommend.mapper.UserPreferenceMapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐系统定时任务 - XXL-Job Handler
 * 定期计算商品相似度矩阵和用户偏好
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendJobHandler {

    private final GoodsInfoMapper goodsInfoMapper;
    private final GoodsSimilarityMapper similarityMapper;
    private final UserBehaviorLogMapper behaviorLogMapper;
    private final UserPreferenceMapper preferenceMapper;
    private final ItemCFAlgorithm itemCFAlgorithm;
    private final ContentBasedAlgorithm contentBasedAlgorithm;

    /**
     * 计算商品相似度矩阵
     * 建议配置：每天凌晨2点执行，CRON: 0 0 2 * * ?
     */
    @XxlJob("computeItemSimilarityHandler")
    public ReturnT<String> computeItemSimilarity() {
        XxlJobHelper.log("开始计算商品相似度...");
        log.info("开始计算商品相似度...");

        try {
            // 获取所有在售商品
            LambdaQueryWrapper<GoodsInfo> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GoodsInfo::getTradeStatus, 0)
                   .in(GoodsInfo::getReviewStatus, 1, 2);
            List<GoodsInfo> goodsList = goodsInfoMapper.selectList(wrapper);

            XxlJobHelper.log("待计算商品数量: {}", goodsList.size());
            log.info("待计算商品数量: {}", goodsList.size());

            // 计算协同过滤相似度
            int cfCount = computeCFSimilarity(goodsList);

            // 计算内容相似度
            int contentCount = computeContentSimilarity(goodsList);

            String result = String.format("相似度计算完成，协同过滤: %d条, 内容相似: %d条", cfCount, contentCount);
            XxlJobHelper.log(result);
            log.info(result);

            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("商品相似度计算失败", e);
            XxlJobHelper.log("计算失败: {}", e.getMessage());
            return ReturnT.FAIL;
        }
    }

    /**
     * 计算协同过滤相似度
     */
    private int computeCFSimilarity(List<GoodsInfo> goodsList) {
        XxlJobHelper.log("计算协同过滤相似度...");

        int count = 0;
        int batchSize = 100;
        List<GoodsSimilarity> batch = new ArrayList<>();

        for (int i = 0; i < goodsList.size(); i++) {
            GoodsInfo goods1 = goodsList.get(i);

            for (int j = i + 1; j < goodsList.size(); j++) {
                GoodsInfo goods2 = goodsList.get(j);

                // 计算相似度
                BigDecimal similarity = itemCFAlgorithm.calculateSimilarity(
                    goods1.getProductId(),
                    goods2.getProductId()
                );

                // 只保存有意义的相似度
                if (similarity.compareTo(BigDecimal.valueOf(0.1)) >= 0) {
                    // 双向保存
                    GoodsSimilarity sim1 = new GoodsSimilarity();
                    sim1.setProductId(goods1.getProductId());
                    sim1.setSimilarProductId(goods2.getProductId());
                    sim1.setSimilarityScore(similarity);
                    sim1.setSimilarityType(GoodsSimilarity.SimilarityType.COLLABORATIVE);
                    batch.add(sim1);

                    GoodsSimilarity sim2 = new GoodsSimilarity();
                    sim2.setProductId(goods2.getProductId());
                    sim2.setSimilarProductId(goods1.getProductId());
                    sim2.setSimilarityScore(similarity);
                    sim2.setSimilarityType(GoodsSimilarity.SimilarityType.COLLABORATIVE);
                    batch.add(sim2);

                    count += 2;
                }

                // 批量保存
                if (batch.size() >= batchSize) {
                    saveBatch(batch);
                    batch.clear();
                }
            }

            // 进度日志
            if (i % 100 == 0) {
                XxlJobHelper.log("协同过滤进度: {}/{}", i, goodsList.size());
            }
        }

        // 保存剩余的
        if (!batch.isEmpty()) {
            saveBatch(batch);
        }

        XxlJobHelper.log("协同过滤相似度计算完成，共{}条记录", count);
        return count;
    }

    /**
     * 计算内容相似度
     */
    private int computeContentSimilarity(List<GoodsInfo> goodsList) {
        XxlJobHelper.log("计算内容相似度...");

        // 按分类分组
        Map<Integer, List<GoodsInfo>> categoryGroups = goodsList.stream()
            .collect(Collectors.groupingBy(GoodsInfo::getCategoryId));

        int count = 0;
        List<GoodsSimilarity> batch = new ArrayList<>();

        for (Map.Entry<Integer, List<GoodsInfo>> entry : categoryGroups.entrySet()) {
            List<GoodsInfo> categoryGoods = entry.getValue();

            // 同分类内计算相似度
            for (int i = 0; i < categoryGoods.size(); i++) {
                GoodsInfo goods1 = categoryGoods.get(i);

                for (int j = i + 1; j < Math.min(i + 50, categoryGoods.size()); j++) {
                    GoodsInfo goods2 = categoryGoods.get(j);

                    BigDecimal similarity = contentBasedAlgorithm.calculateContentSimilarity(goods1, goods2);

                    if (similarity.compareTo(BigDecimal.valueOf(0.3)) >= 0) {
                        // 双向保存
                        GoodsSimilarity sim1 = new GoodsSimilarity();
                        sim1.setProductId(goods1.getProductId());
                        sim1.setSimilarProductId(goods2.getProductId());
                        sim1.setSimilarityScore(similarity);
                        sim1.setSimilarityType(GoodsSimilarity.SimilarityType.CONTENT_BASED);
                        batch.add(sim1);

                        GoodsSimilarity sim2 = new GoodsSimilarity();
                        sim2.setProductId(goods2.getProductId());
                        sim2.setSimilarProductId(goods1.getProductId());
                        sim2.setSimilarityScore(similarity);
                        sim2.setSimilarityType(GoodsSimilarity.SimilarityType.CONTENT_BASED);
                        batch.add(sim2);

                        count += 2;
                    }

                    if (batch.size() >= 100) {
                        saveBatch(batch);
                        batch.clear();
                    }
                }
            }
        }

        if (!batch.isEmpty()) {
            saveBatch(batch);
        }

        XxlJobHelper.log("内容相似度计算完成，共{}条记录", count);
        return count;
    }

    /**
     * 批量保存
     */
    private void saveBatch(List<GoodsSimilarity> batch) {
        for (GoodsSimilarity sim : batch) {
            LambdaQueryWrapper<GoodsSimilarity> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(GoodsSimilarity::getProductId, sim.getProductId())
                   .eq(GoodsSimilarity::getSimilarProductId, sim.getSimilarProductId())
                   .eq(GoodsSimilarity::getSimilarityType, sim.getSimilarityType());

            GoodsSimilarity existing = similarityMapper.selectOne(wrapper);
            if (existing != null) {
                existing.setSimilarityScore(sim.getSimilarityScore());
                similarityMapper.updateById(existing);
            } else {
                similarityMapper.insert(sim);
            }
        }
    }

    /**
     * 更新用户偏好画像
     * 建议配置：每天凌晨3点执行，CRON: 0 0 3 * * ?
     */
    @XxlJob("computeUserPreferenceHandler")
    public ReturnT<String> computeUserPreference() {
        XxlJobHelper.log("开始更新用户偏好...");
        log.info("开始更新用户偏好...");

        try {
            // 获取所有有行为的用户
            List<Long> userIds = behaviorLogMapper.selectRecentProductIds(null, 10000);
            Set<Long> uniqueUsers = new HashSet<>(userIds);

            XxlJobHelper.log("待更新用户数量: {}", uniqueUsers.size());
            log.info("待更新用户数量: {}", uniqueUsers.size());

            int successCount = 0;
            for (Long userId : uniqueUsers) {
                try {
                    updateUserPreference(userId);
                    successCount++;
                } catch (Exception e) {
                    XxlJobHelper.log("更新用户偏好失败: userId={}, error={}", userId, e.getMessage());
                }
            }

            String result = String.format("用户偏好更新完成，成功: %d/%d", successCount, uniqueUsers.size());
            XxlJobHelper.log(result);
            log.info(result);

            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("用户偏好更新失败", e);
            XxlJobHelper.log("更新失败: {}", e.getMessage());
            return ReturnT.FAIL;
        }
    }

    /**
     * 更新单个用户的偏好
     */
    private void updateUserPreference(Long userId) {
        // 获取分类偏好
        List<Map<String, Object>> categoryPrefs = behaviorLogMapper.selectCategoryPreference(userId);

        Map<String, Double> categoryScores = new HashMap<>();
        double maxScore = 0;
        for (Map<String, Object> pref : categoryPrefs) {
            Integer categoryId = (Integer) pref.get("category_id");
            Number score = (Number) pref.get("score");
            if (categoryId != null && score != null) {
                maxScore = Math.max(maxScore, score.doubleValue());
            }
        }

        // 归一化
        for (Map<String, Object> pref : categoryPrefs) {
            Integer categoryId = (Integer) pref.get("category_id");
            Number score = (Number) pref.get("score");
            if (categoryId != null && score != null && maxScore > 0) {
                categoryScores.put(categoryId.toString(), score.doubleValue() / maxScore);
            }
        }

        // 保存或更新
        UserPreference preference = preferenceMapper.selectById(userId);
        boolean exists = preference != null;
        if (preference == null) {
            preference = new UserPreference();
            preference.setUserId(userId);
        }

        preference.setCategoryScores(JSON.toJSONString(categoryScores));
        preference.setLastActiveTime(LocalDateTime.now());

        if (exists) {
            preferenceMapper.updateById(preference);
        } else {
            preferenceMapper.insert(preference);
        }
    }

    /**
     * 清理过期行为日志
     * 建议配置：每周日凌晨4点执行，CRON: 0 0 4 ? * SUN
     */
    @XxlJob("cleanExpiredBehaviorLogHandler")
    public ReturnT<String> cleanExpiredBehaviorLog() {
        XxlJobHelper.log("开始清理过期行为日志...");
        log.info("开始清理过期行为日志...");

        try {
            // 删除90天前的日志
            int deleted = behaviorLogMapper.delete(
                new LambdaQueryWrapper<UserBehaviorLog>()
                    .lt(UserBehaviorLog::getCreateTime,
                        LocalDateTime.now().minusDays(90))
            );

            String result = String.format("清理完成，删除 %d 条过期日志", deleted);
            XxlJobHelper.log(result);
            log.info(result);

            return ReturnT.SUCCESS;
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
            XxlJobHelper.log("清理失败: {}", e.getMessage());
            return ReturnT.FAIL;
        }
    }
}
