package com.unimarket.module.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.risk.entity.RiskBehaviorControl;
import com.unimarket.module.risk.enums.RiskAction;
import com.unimarket.module.risk.mapper.RiskBehaviorControlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 用户行为管控查询服务。
 */
@Service
@RequiredArgsConstructor
public class RiskBehaviorControlService {

    public static final String EVENT_ALL = "ALL";
    private static final int STATUS_ENABLED = 1;
    private static final String CACHE_KEY_PREFIX = "risk:cache:behavior:";
    private static final String NULL_SENTINEL = "__NULL__";
    private static final long CACHE_TTL_SECONDS = 60L;

    private final RiskBehaviorControlMapper riskBehaviorControlMapper;
    private final RedisCache redisCache;

    public boolean isAllowed(Long userId, String eventType) {
        return !isHardBlocked(userId, eventType);
    }

    public boolean isHardBlocked(Long userId, String eventType) {
        RiskBehaviorControl matched = findMatchedControl(userId, eventType);
        if (matched == null) {
            return false;
        }
        return RiskAction.from(matched.getControlAction()) == RiskAction.REJECT;
    }

    public RiskBehaviorControl findMatchedControl(Long userId, String eventType) {
        if (userId == null) {
            return null;
        }

        String normalizedEventType = normalizeEventType(eventType);
        if (normalizedEventType != null) {
            RiskBehaviorControl exact = getCachedControl(userId, normalizedEventType);
            if (exact != null) {
                return exact;
            }
        }

        return getCachedControl(userId, EVENT_ALL);
    }

    public void evictControl(Long userId, String eventType) {
        if (userId == null) {
            return;
        }
        String normalizedEventType = normalizeEventType(eventType);
        if (normalizedEventType != null) {
            redisCache.delete(buildCacheKey(userId, normalizedEventType));
        }
        redisCache.delete(buildCacheKey(userId, EVENT_ALL));
    }

    private RiskBehaviorControl getCachedControl(Long userId, String eventType) {
        String cacheKey = buildCacheKey(userId, eventType);
        Object cached = redisCache.get(cacheKey);
        if (cached != null) {
            if (cached instanceof RiskBehaviorControl control) {
                return control;
            }
            if (NULL_SENTINEL.equals(String.valueOf(cached))) {
                return null;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        RiskBehaviorControl control = riskBehaviorControlMapper.selectOne(activeQuery(userId, eventType, now));
        if (control == null) {
            redisCache.set(cacheKey, NULL_SENTINEL, CACHE_TTL_SECONDS);
            return null;
        }
        redisCache.set(cacheKey, control, resolveTtlSeconds(control.getExpireTime()));
        return control;
    }

    private LambdaQueryWrapper<RiskBehaviorControl> activeQuery(Long userId, String eventType, LocalDateTime now) {
        return new LambdaQueryWrapper<RiskBehaviorControl>()
                .eq(RiskBehaviorControl::getUserId, userId)
                .eq(RiskBehaviorControl::getEventType, eventType)
                .eq(RiskBehaviorControl::getStatus, STATUS_ENABLED)
                .and(w -> w.isNull(RiskBehaviorControl::getExpireTime)
                        .or()
                        .gt(RiskBehaviorControl::getExpireTime, now))
                .orderByDesc(RiskBehaviorControl::getUpdateTime)
                .orderByDesc(RiskBehaviorControl::getId)
                .last("LIMIT 1");
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null) {
            return null;
        }
        String normalized = eventType.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildCacheKey(Long userId, String eventType) {
        return CACHE_KEY_PREFIX + userId + ":" + eventType;
    }

    private long resolveTtlSeconds(LocalDateTime expireTime) {
        if (expireTime == null) {
            return CACHE_TTL_SECONDS;
        }
        long seconds = Duration.between(LocalDateTime.now(), expireTime).toSeconds();
        return Math.max(1L, Math.min(seconds, CACHE_TTL_SECONDS));
    }
}
