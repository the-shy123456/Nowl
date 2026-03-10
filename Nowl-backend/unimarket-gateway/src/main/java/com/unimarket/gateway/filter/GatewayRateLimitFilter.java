package com.unimarket.gateway.filter;

import com.unimarket.gateway.support.GatewayClientIpResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关限流过滤器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {

    private static final long WINDOW_MS = 60_000L;
    private static final long REDIS_KEY_TTL_MS = WINDOW_MS * 2;
    private static final int ADMIN_LIMIT = 240;
    private static final int LOGIN_LIMIT = 60;
    private static final int DEFAULT_LIMIT = 600;

    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>();

    static {
        INCREMENT_SCRIPT.setResultType(Long.class);
        INCREMENT_SCRIPT.setScriptText("local current = redis.call('INCR', KEYS[1]) "
                + "if current == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end "
                + "return current");
    }

    private final Map<String, WindowCounter> fallbackCounters = new ConcurrentHashMap<>();
    private final GatewayClientIpResolver gatewayClientIpResolver;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String bucket = resolveBucket(path);
        if (bucket == null) {
            return chain.filter(exchange);
        }

        String ip = gatewayClientIpResolver.resolve(exchange);
        int limit = resolveLimit(bucket);
        long current;
        long now = Instant.now().toEpochMilli();
        try {
            current = incrementDistributedCounter(bucket, ip, now);
        } catch (Exception ex) {
            current = incrementLocalFallback(bucket, ip, now);
            log.warn("Redis 限流失败，已降级为本地限流，bucket={}, ip={}", bucket, ip, ex);
        }

        if (current > limit) {
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");
            exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
            exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", "0");
            String payload = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}";
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(payload.getBytes(StandardCharsets.UTF_8))));
        }

        long remaining = Math.max(limit - current, 0);
        exchange.getResponse().getHeaders().set("X-RateLimit-Limit", String.valueOf(limit));
        exchange.getResponse().getHeaders().set("X-RateLimit-Remaining", String.valueOf(remaining));
        return chain.filter(exchange);
    }

    private long incrementDistributedCounter(String bucket, String ip, long now) {
        String key = buildRedisKey(bucket, ip, now);
        Long total = stringRedisTemplate.execute(
                INCREMENT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(REDIS_KEY_TTL_MS)
        );
        return total == null ? 0L : total;
    }

    private long incrementLocalFallback(String bucket, String ip, long now) {
        String key = bucket + "|" + ip;
        WindowCounter counter = fallbackCounters.computeIfAbsent(key, k -> new WindowCounter(now, 0));
        synchronized (counter) {
            if (now - counter.windowStart >= WINDOW_MS) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count++;
            if (fallbackCounters.size() > 10_000) {
                cleanupExpired(now);
            }
            return counter.count;
        }
    }

    private String resolveBucket(String path) {
        if (path.startsWith("/admin/")) {
            return "ADMIN";
        }
        if (path.startsWith("/auth/login")) {
            return "LOGIN";
        }
        if (path.startsWith("/chat/send")) {
            return "CHAT";
        }
        if (path.startsWith("/ai/chat")) {
            return "AI";
        }
        return null;
    }

    private int resolveLimit(String bucket) {
        if ("ADMIN".equals(bucket)) {
            return ADMIN_LIMIT;
        }
        if ("LOGIN".equals(bucket)) {
            return LOGIN_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    private String buildRedisKey(String bucket, String ip, long now) {
        long window = now / WINDOW_MS;
        return "gateway:rate:" + bucket + ":" + ip + ":" + window;
    }

    private void cleanupExpired(long now) {
        fallbackCounters.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= WINDOW_MS * 2);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private static final class WindowCounter {
        private long windowStart;
        private long count;

        private WindowCounter(long windowStart, long count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
