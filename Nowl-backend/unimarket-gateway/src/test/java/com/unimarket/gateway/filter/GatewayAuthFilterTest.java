package com.unimarket.gateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewayAuthFilterTest {

    private final GatewayAuthFilter filter = new GatewayAuthFilter();

    @Test
    @DisplayName("admin path without auth should return 401")
    void adminPath_withoutAuth_unauthorized() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/admin/risk/events").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        GatewayFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertFalse(chainInvoked.get());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("admin path with access_token cookie should pass")
    void adminPath_withAccessCookie_pass() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/admin/risk/events")
                .cookie(new HttpCookie("access_token", "token-xxx"))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);
        AtomicBoolean chainInvoked = new AtomicBoolean(false);

        GatewayFilterChain chain = ex -> {
            chainInvoked.set(true);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertTrue(chainInvoked.get());
    }
}
