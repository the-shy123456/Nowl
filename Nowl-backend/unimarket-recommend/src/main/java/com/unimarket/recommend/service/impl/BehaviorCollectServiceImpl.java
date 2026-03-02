package com.unimarket.recommend.service.impl;

import com.unimarket.module.goods.entity.GoodsInfo;
import com.unimarket.module.goods.mapper.GoodsInfoMapper;
import com.unimarket.recommend.entity.UserBehaviorLog;
import com.unimarket.recommend.mapper.UserBehaviorLogMapper;
import com.unimarket.recommend.service.BehaviorCollectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户行为收集服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BehaviorCollectServiceImpl implements BehaviorCollectService {

    private final UserBehaviorLogMapper behaviorLogMapper;
    private final GoodsInfoMapper goodsInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String VIEW_COUNT_KEY = "goods:view:";
    private static final String HOT_SCORE_KEY = "goods:hot:";

    @Override
    @Async
    public void recordView(Long userId, Long productId, Integer categoryId, Integer duration) {
        if (productId == null) {
            return;
        }

        try {
            // 1. 记录行为日志
            if (userId != null) {
                UserBehaviorLog behaviorLog = new UserBehaviorLog();
                behaviorLog.setUserId(userId);
                behaviorLog.setBehaviorType(UserBehaviorLog.BehaviorType.VIEW);
                behaviorLog.setProductId(productId);
                behaviorLog.setCategoryId(categoryId);
                behaviorLog.setDuration(duration);
                behaviorLogMapper.insert(behaviorLog);
            }

            // 2. 增加浏览量（使用Redis计数）
            String viewKey = VIEW_COUNT_KEY + productId;
            Long viewCount = stringRedisTemplate.opsForValue().increment(viewKey);

            // 设置过期时间（防止无限增长）
            if (viewCount != null && viewCount == 1) {
                stringRedisTemplate.expire(viewKey, 24, TimeUnit.HOURS);
            }

            // 3. 更新热度分
            updateHotScore(productId);

            log.debug("记录浏览行为: userId={}, productId={}", userId, productId);
        } catch (Exception e) {
            log.error("记录浏览行为失败: productId={}", productId, e);
        }
    }

    @Override
    @Async
    public void recordCollect(Long userId, Long productId, Integer categoryId) {
        if (userId == null || productId == null) {
            return;
        }

        try {
            UserBehaviorLog behaviorLog = new UserBehaviorLog();
            behaviorLog.setUserId(userId);
            behaviorLog.setBehaviorType(UserBehaviorLog.BehaviorType.COLLECT);
            behaviorLog.setProductId(productId);
            behaviorLog.setCategoryId(categoryId);
            behaviorLogMapper.insert(behaviorLog);

            // 更新热度分
            updateHotScore(productId);

            log.debug("记录收藏行为: userId={}, productId={}", userId, productId);
        } catch (Exception e) {
            log.error("记录收藏行为失败: productId={}", productId, e);
        }
    }

    @Override
    @Async
    public void recordBuy(Long userId, Long productId, Integer categoryId) {
        if (userId == null || productId == null) {
            return;
        }

        try {
            UserBehaviorLog behaviorLog = new UserBehaviorLog();
            behaviorLog.setUserId(userId);
            behaviorLog.setBehaviorType(UserBehaviorLog.BehaviorType.BUY);
            behaviorLog.setProductId(productId);
            behaviorLog.setCategoryId(categoryId);
            behaviorLogMapper.insert(behaviorLog);

            // 更新热度分
            updateHotScore(productId);

            log.debug("记录购买行为: userId={}, productId={}", userId, productId);
        } catch (Exception e) {
            log.error("记录购买行为失败: productId={}", productId, e);
        }
    }

    @Override
    @Async
    public void recordSearch(Long userId, String keyword) {
        if (userId == null || keyword == null || keyword.isEmpty()) {
            return;
        }

        try {
            UserBehaviorLog behaviorLog = new UserBehaviorLog();
            behaviorLog.setUserId(userId);
            behaviorLog.setBehaviorType(UserBehaviorLog.BehaviorType.SEARCH);
            behaviorLog.setKeyword(keyword);
            behaviorLogMapper.insert(behaviorLog);

            log.debug("记录搜索行为: userId={}, keyword={}", userId, keyword);
        } catch (Exception e) {
            log.error("记录搜索行为失败", e);
        }
    }

    @Override
    public void updateHotScore(Long productId) {
        if (productId == null) {
            return;
        }

        try {
            // 计算热度分 = 浏览数 * 1 + 收藏数 * 3 + 购买数 * 5
            GoodsInfo goods = goodsInfoMapper.selectById(productId);
            if (goods == null) {
                return;
            }

            // 从Redis获取实时浏览量
            String viewKey = VIEW_COUNT_KEY + productId;
            String viewCountStr = stringRedisTemplate.opsForValue().get(viewKey);
            int viewCount = viewCountStr != null ? Integer.parseInt(viewCountStr) : 0;

            // 计算热度分
            int collectCount = goods.getCollectCount() != null ? goods.getCollectCount() : 0;
            double hotScore = viewCount * 1.0 + collectCount * 3.0;

            // 更新到Redis（用于快速排序）
            String hotKey = HOT_SCORE_KEY + "all";
            stringRedisTemplate.opsForZSet().add(hotKey, productId.toString(), hotScore);

        } catch (Exception e) {
            log.error("更新热度分失败: productId={}", productId, e);
        }
    }
}
