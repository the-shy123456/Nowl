package com.unimarket.gateway.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GatewayClientIpResolverTest {

    private final GatewayClientIpResolver resolver =
            new GatewayClientIpResolver("127.0.0.1,::1,0:0:0:0:0:0:0:1");

    @Test
    @DisplayName("未命中可信代理时忽略X-Forwarded-For")
    void shouldIgnoreForwardedHeadersWhenRemoteIsUntrusted() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("203.0.113.7", 34567))
                .header("X-Forwarded-For", "198.51.100.1")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String resolved = resolver.resolve(exchange);

        assertEquals("203.0.113.7", resolved);
    }

    @Test
    @DisplayName("命中可信代理时优先使用X-Forwarded-For首跳")
    void shouldUseFirstXForwardedForWhenRemoteIsTrusted() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 34567))
                .header("X-Forwarded-For", "198.51.100.1, 10.0.0.2")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String resolved = resolver.resolve(exchange);

        assertEquals("198.51.100.1", resolved);
    }

    @Test
    @DisplayName("命中可信代理且XFF缺失时回退X-Real-IP")
    void shouldFallbackToXRealIpWhenXffMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 34567))
                .header("X-Real-IP", "198.51.100.8")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String resolved = resolver.resolve(exchange);

        assertEquals("198.51.100.8", resolved);
    }

    @Test
    @DisplayName("解析时归一化IPv6映射地址")
    void shouldNormalizeIpv6MappedAddress() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 34567))
                .header("X-Forwarded-For", "::ffff:198.51.100.11")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        String resolved = resolver.resolve(exchange);

        assertEquals("198.51.100.11", resolved);
    }
}

