package com.unimarket.module.risk.service;

import com.unimarket.module.risk.dto.RiskContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 风控实时态存储。
 */
@Service
@RequiredArgsConstructor
public class RiskRealtimeStore {

    private static final Duration EVENT_TTL = Duration.ofHours(24);
    private static final Duration DEVICE_TTL = Duration.ofHours(24);
    private static final Duration LOGIN_TTL = Duration.ofHours(1);

    private final StringRedisTemplate stringRedisTemplate;

    public void recordEvaluation(RiskContext context, String traceId, LocalDateTime eventTime) {
        long now = toEpochMillis(eventTime);
        recordEventCounter(context.getEventType(), context.getSubjectType(), context.getSubjectId(), traceId, now);

        if (context.getRequestIp() != null
                && !context.getRequestIp().isBlank()
                && (!"IP".equalsIgnoreCase(context.getSubjectType())
                || !context.getRequestIp().equals(context.getSubjectId()))) {
            recordEventCounter(context.getEventType(), "IP", context.getRequestIp(), traceId, now);
        }

        Object fingerprintObj = context.getFeatures().get("deviceFingerprint");
        if (fingerprintObj != null
                && context.getSubjectId() != null
                && !context.getSubjectId().isBlank()) {
            String fingerprint = String.valueOf(fingerprintObj);
            if (!fingerprint.isBlank()) {
                String key = buildDeviceKey(fingerprint);
                stringRedisTemplate.opsForZSet().add(key, context.getSubjectId(), now);
                stringRedisTemplate.expire(key, DEVICE_TTL);
                clearExpiredSamples(key, now - DEVICE_TTL.toMillis());
            }
        }
    }

    public long countEvents(String eventType, String subjectType, String subjectId, int windowMinutes) {
        if (eventType == null || subjectType == null || subjectId == null
                || eventType.isBlank() || subjectType.isBlank() || subjectId.isBlank()) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long start = now - Math.max(windowMinutes, 1) * 60_000L;
        String key = buildEventKey(eventType, subjectType, subjectId);
        clearExpiredSamples(key, now - EVENT_TTL.toMillis());
        Long total = stringRedisTemplate.opsForZSet().count(key, start, now);
        return total == null ? 0L : total;
    }

    public void recordLoginOutcome(String ip, String result) {
        if (ip == null || ip.isBlank() || result == null || result.isBlank()) {
            return;
        }
        String normalized = result.trim().toUpperCase();
        if (!"FAIL".equals(normalized) && !"CHALLENGE".equals(normalized)) {
            return;
        }
        long now = System.currentTimeMillis();
        String key = buildLoginKey(ip);
        stringRedisTemplate.opsForZSet().add(key, now + ":" + UUID.randomUUID(), now);
        stringRedisTemplate.expire(key, LOGIN_TTL);
        clearExpiredSamples(key, now - LOGIN_TTL.toMillis());
    }

    public long countLoginFailures(String ip, int windowMinutes) {
        if (ip == null || ip.isBlank()) {
            return 0L;
        }
        long now = System.currentTimeMillis();
        long start = now - Math.max(windowMinutes, 1) * 60_000L;
        String key = buildLoginKey(ip);
        clearExpiredSamples(key, now - LOGIN_TTL.toMillis());
        Long total = stringRedisTemplate.opsForZSet().count(key, start, now);
        return total == null ? 0L : total;
    }

    public int countDeviceSubjects(String fingerprint, int windowMinutes) {
        if (fingerprint == null || fingerprint.isBlank()) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long start = now - Math.max(windowMinutes, 1) * 60_000L;
        String key = buildDeviceKey(fingerprint);
        clearExpiredSamples(key, now - DEVICE_TTL.toMillis());
        Long total = stringRedisTemplate.opsForZSet().count(key, start, now);
        return total == null ? 0 : total.intValue();
    }

    private void recordEventCounter(String eventType,
                                    String subjectType,
                                    String subjectId,
                                    String traceId,
                                    long timestamp) {
        if (eventType == null || subjectType == null || subjectId == null
                || eventType.isBlank() || subjectType.isBlank() || subjectId.isBlank()) {
            return;
        }
        String key = buildEventKey(eventType, subjectType, subjectId);
        stringRedisTemplate.opsForZSet().add(key, traceId + ":" + UUID.randomUUID(), timestamp);
        stringRedisTemplate.expire(key, EVENT_TTL);
        clearExpiredSamples(key, timestamp - EVENT_TTL.toMillis());
    }

    private void clearExpiredSamples(String key, long minTimestamp) {
        stringRedisTemplate.opsForZSet().removeRangeByScore(key, 0, minTimestamp - 1);
    }

    private long toEpochMillis(LocalDateTime time) {
        LocalDateTime target = time == null ? LocalDateTime.now() : time;
        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private String buildEventKey(String eventType, String subjectType, String subjectId) {
        return "risk:event:" + eventType.trim().toUpperCase()
                + ":" + subjectType.trim().toUpperCase()
                + ":" + subjectId.trim();
    }

    private String buildDeviceKey(String fingerprint) {
        return "risk:device:" + fingerprint.trim();
    }

    private String buildLoginKey(String ip) {
        return "risk:login:ip:" + ip.trim();
    }
}
