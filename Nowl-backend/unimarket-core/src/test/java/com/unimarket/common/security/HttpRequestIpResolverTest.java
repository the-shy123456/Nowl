package com.unimarket.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpRequestIpResolverTest {

    private final HttpRequestIpResolver resolver =
            new HttpRequestIpResolver("127.0.0.1,::1,0:0:0:0:0:0:0:1");

    @Test
    @DisplayName("未命中可信代理时忽略X-Forwarded-For")
    void shouldIgnoreForwardedHeadersWhenRemoteIsUntrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("X-Forwarded-For", "198.51.100.1");

        String resolved = resolver.resolve(request);

        assertEquals("203.0.113.7", resolved);
    }

    @Test
    @DisplayName("命中可信代理时优先使用X-Forwarded-For首跳")
    void shouldUseFirstXForwardedForWhenRemoteIsTrusted() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.1, 10.0.0.2");

        String resolved = resolver.resolve(request);

        assertEquals("198.51.100.1", resolved);
    }

    @Test
    @DisplayName("命中可信代理且XFF缺失时回退X-Real-IP")
    void shouldFallbackToXRealIpWhenXffMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.8");

        String resolved = resolver.resolve(request);

        assertEquals("198.51.100.8", resolved);
    }

    @Test
    @DisplayName("解析时归一化IPv6映射地址")
    void shouldNormalizeIpv6MappedAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("::ffff:127.0.0.1");
        request.addHeader("X-Forwarded-For", "::ffff:198.51.100.11");

        String resolved = resolver.resolve(request);

        assertEquals("198.51.100.11", resolved);
    }

    @Test
    @DisplayName("空请求返回null")
    void shouldReturnNullWhenRequestIsNull() {
        assertNull(resolver.resolve(null));
    }
}

