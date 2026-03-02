package com.unimarket.gateway.filter;

import com.unimarket.gateway.support.GatewayClientIpResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class GatewayRateLimitFilter implements GlobalFilter, Ordered {

    private static final long WINDOW_MS = 60_000L;
    private static final int ADMIN_LIMIT = 240;
    private static final int LOGIN_LIMIT = 60;
    private static final int DEFAULT_LIMIT = 600;

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final GatewayClientIpResolver gatewayClientIpResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!needRateLimit(path)) {
            return chain.filter(exchange);
        }

        String ip = gatewayClientIpResolver.resolve(exchange);
        String key = path + "|" + ip;
        int limit = resolveLimit(path);

        long now = Instant.now().toEpochMilli();
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter(now, 0));
        synchronized (counter) {
            if (now - counter.windowStart >= WINDOW_MS) {
                counter.windowStart = now;
                counter.count = 0;
            }
            counter.count++;
            if (counter.count > limit) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");
                String payload = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}";
                return exchange.getResponse().writeWith(Mono.just(
                        exchange.getResponse().bufferFactory().wrap(payload.getBytes(StandardCharsets.UTF_8))));
            }
        }

        if (counters.size() > 10_000) {
            cleanupExpired(now);
        }

        return chain.filter(exchange);
    }

    private boolean needRateLimit(String path) {
        return path.startsWith("/admin/")
                || path.startsWith("/auth/login")
                || path.startsWith("/chat/send")
                || path.startsWith("/ai/chat");
    }

    private int resolveLimit(String path) {
        if (path.startsWith("/admin/")) {
            return ADMIN_LIMIT;
        }
        if (path.startsWith("/auth/login")) {
            return LOGIN_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    private void cleanupExpired(long now) {
        counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart >= WINDOW_MS * 2);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private static final class WindowCounter {
        private long windowStart;
        private int count;

        private WindowCounter(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
