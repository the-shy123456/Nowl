package com.unimarket.gateway.filter;

import com.unimarket.gateway.support.GatewayClientIpResolver;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayAuditFilter implements GlobalFilter, Ordered {

    private final MeterRegistry meterRegistry;
    private final GatewayClientIpResolver gatewayClientIpResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod() == null ? "UNKNOWN" : exchange.getRequest().getMethod().name();

        return chain.filter(exchange).doFinally(signalType -> {
            long durationNs = System.nanoTime() - start;
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            int statusCode = status == null ? 200 : status.value();
            String traceId = String.valueOf(exchange.getAttributeOrDefault(GatewayTraceFilter.TRACE_ID_ATTR, "-"));

            meterRegistry.counter("unimarket.gateway.request.total",
                    "method", method,
                    "path", normalizePath(path),
                    "status", String.valueOf(statusCode)).increment();
            Timer.builder("unimarket.gateway.request.duration")
                    .tag("method", method)
                    .tag("path", normalizePath(path))
                    .register(meterRegistry)
                    .record(durationNs, TimeUnit.NANOSECONDS);

            if (path.startsWith("/admin/") || statusCode >= 400) {
                String ip = gatewayClientIpResolver.resolve(exchange);
                log.info("gateway_audit traceId={} method={} path={} status={} costMs={} ip={}",
                        traceId, method, path, statusCode, durationNs / 1_000_000, ip);
            }
        });
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "unknown";
        }
        if (path.startsWith("/admin/")) {
            String[] segments = path.split("/");
            if (segments.length >= 3) {
                return "/admin/" + segments[2] + "/*";
            }
            return "/admin/*";
        }
        if (path.startsWith("/auth/")) {
            return "/auth/*";
        }
        return path;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 10;
    }

}
