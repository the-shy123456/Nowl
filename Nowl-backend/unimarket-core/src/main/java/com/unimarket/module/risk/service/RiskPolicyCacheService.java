package com.unimarket.module.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.unimarket.common.utils.RedisCache;
import com.unimarket.module.risk.entity.RiskBlacklist;
import com.unimarket.module.risk.entity.RiskRule;
import com.unimarket.module.risk.entity.RiskWhitelist;
import com.unimarket.module.risk.mapper.RiskBlacklistMapper;
import com.unimarket.module.risk.mapper.RiskRuleMapper;
import com.unimarket.module.risk.mapper.RiskWhitelistMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 风控配置缓存服务。
 */
@Service
@RequiredArgsConstructor
public class RiskPolicyCacheService {

    private static final String RULE_KEY_PREFIX = "risk:cache:rule:";
    private static final String BLACKLIST_KEY_PREFIX = "risk:cache:blacklist:";
    private static final String WHITELIST_KEY_PREFIX = "risk:cache:whitelist:";
    private static final String NULL_SENTINEL = "__NULL__";
    private static final long RULE_TTL_SECONDS = 60L;
    private static final long SUBJECT_TTL_SECONDS = 60L;

    private final RedisCache redisCache;
    private final RiskRuleMapper riskRuleMapper;
    private final RiskBlacklistMapper riskBlacklistMapper;
    private final RiskWhitelistMapper riskWhitelistMapper;

    public List<RiskRule> getEnabledRules(String eventType) {
        String normalizedEventType = normalize(eventType);
        if (normalizedEventType == null) {
            return Collections.emptyList();
        }
        String cacheKey = RULE_KEY_PREFIX + normalizedEventType;
        List<RiskRule> cached = redisCache.getCacheList(cacheKey);
        if (cached != null) {
            return cached;
        }
        List<RiskRule> rules = riskRuleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getEventType, normalizedEventType)
                .eq(RiskRule::getStatus, 1)
                .orderByAsc(RiskRule::getPriority)
                .orderByAsc(RiskRule::getRuleId));
        redisCache.setCacheList(cacheKey, rules, RULE_TTL_SECONDS, TimeUnit.SECONDS);
        return rules;
    }

    public boolean isWhitelisted(String subjectType, String subjectId) {
        String cacheKey = buildSubjectKey(WHITELIST_KEY_PREFIX, subjectType, subjectId);
        if (cacheKey == null) {
            return false;
        }
        Object cached = redisCache.get(cacheKey);
        if (cached != null) {
            return !NULL_SENTINEL.equals(String.valueOf(cached));
        }
        LocalDateTime now = LocalDateTime.now();
        RiskWhitelist whitelist = riskWhitelistMapper.selectOne(new LambdaQueryWrapper<RiskWhitelist>()
                .eq(RiskWhitelist::getSubjectType, normalize(subjectType))
                .eq(RiskWhitelist::getSubjectId, subjectId.trim())
                .eq(RiskWhitelist::getStatus, 1)
                .and(w -> w.isNull(RiskWhitelist::getExpireTime)
                        .or()
                        .gt(RiskWhitelist::getExpireTime, now))
                .last("LIMIT 1"));
        if (whitelist == null) {
            redisCache.set(cacheKey, NULL_SENTINEL, SUBJECT_TTL_SECONDS);
            return false;
        }
        redisCache.set(cacheKey, whitelist, resolveTtlSeconds(whitelist.getExpireTime()));
        return true;
    }

    public RiskBlacklist getActiveBlacklist(String subjectType, String subjectId) {
        String cacheKey = buildSubjectKey(BLACKLIST_KEY_PREFIX, subjectType, subjectId);
        if (cacheKey == null) {
            return null;
        }
        Object cached = redisCache.get(cacheKey);
        if (cached != null) {
            if (cached instanceof RiskBlacklist riskBlacklist) {
                return riskBlacklist;
            }
            if (NULL_SENTINEL.equals(String.valueOf(cached))) {
                return null;
            }
        }
        LocalDateTime now = LocalDateTime.now();
        RiskBlacklist blacklist = riskBlacklistMapper.selectOne(new LambdaQueryWrapper<RiskBlacklist>()
                .eq(RiskBlacklist::getSubjectType, normalize(subjectType))
                .eq(RiskBlacklist::getSubjectId, subjectId.trim())
                .eq(RiskBlacklist::getStatus, 1)
                .and(w -> w.isNull(RiskBlacklist::getExpireTime)
                        .or()
                        .gt(RiskBlacklist::getExpireTime, now))
                .last("LIMIT 1"));
        if (blacklist == null) {
            redisCache.set(cacheKey, NULL_SENTINEL, SUBJECT_TTL_SECONDS);
            return null;
        }
        redisCache.set(cacheKey, blacklist, resolveTtlSeconds(blacklist.getExpireTime()));
        return blacklist;
    }

    public void evictRules(String eventType) {
        String normalizedEventType = normalize(eventType);
        if (normalizedEventType == null) {
            return;
        }
        redisCache.delete(RULE_KEY_PREFIX + normalizedEventType);
    }

    public void evictBlacklist(String subjectType, String subjectId) {
        evictSubjectKey(BLACKLIST_KEY_PREFIX, subjectType, subjectId);
    }

    public void evictWhitelist(String subjectType, String subjectId) {
        evictSubjectKey(WHITELIST_KEY_PREFIX, subjectType, subjectId);
    }

    private void evictSubjectKey(String prefix, String subjectType, String subjectId) {
        String cacheKey = buildSubjectKey(prefix, subjectType, subjectId);
        if (cacheKey != null) {
            redisCache.delete(cacheKey);
        }
    }

    private String buildSubjectKey(String prefix, String subjectType, String subjectId) {
        String normalizedType = normalize(subjectType);
        if (normalizedType == null || subjectId == null || subjectId.isBlank()) {
            return null;
        }
        return prefix + normalizedType + ":" + subjectId.trim();
    }

    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase();
    }

    private long resolveTtlSeconds(LocalDateTime expireTime) {
        if (expireTime == null) {
            return SUBJECT_TTL_SECONDS;
        }
        long seconds = Duration.between(LocalDateTime.now(), expireTime).toSeconds();
        return Math.max(1L, Math.min(seconds, SUBJECT_TTL_SECONDS));
    }
}

