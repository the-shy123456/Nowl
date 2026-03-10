package com.unimarket.module.risk.service;

import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.risk.enums.RiskMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 风控模式服务。
 */
@Service
@RequiredArgsConstructor
public class RiskModeService {

    private static final String RISK_MODE_KEY = "risk:mode:global";
    private static final long LOCAL_CACHE_MS = 1_000L;

    private final RedisCache redisCache;

    private volatile RiskMode cachedMode = RiskMode.FULL;
    private volatile long cacheExpireAt = 0L;

    public RiskMode getMode() {
        long now = System.currentTimeMillis();
        if (now < cacheExpireAt) {
            return cachedMode;
        }
        RiskMode mode = RiskMode.from(redisCache.get(RISK_MODE_KEY, String.class));
        cachedMode = mode;
        cacheExpireAt = now + LOCAL_CACHE_MS;
        return mode;
    }

    public void setMode(RiskMode mode) {
        RiskMode target = mode == null ? RiskMode.FULL : mode;
        redisCache.set(RISK_MODE_KEY, target.name());
        cachedMode = target;
        cacheExpireAt = System.currentTimeMillis() + LOCAL_CACHE_MS;
    }
}
