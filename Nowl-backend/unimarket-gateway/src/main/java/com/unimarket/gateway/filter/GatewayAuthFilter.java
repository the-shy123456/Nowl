package com.unimarket.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (!path.startsWith("/admin/")) {
            return chain.filter(exchange);
        }

        String bearer = exchange.getRequest().getHeaders().getFirst("Authorization");
        HttpCookie accessCookie = exchange.getRequest().getCookies().getFirst("access_token");
        boolean hasBearer = bearer != null && bearer.startsWith("Bearer ") && bearer.length() > 7;
        boolean hasCookie = accessCookie != null && accessCookie.getValue() != null && !accessCookie.getValue().isBlank();

        if (hasBearer || hasCookie) {
            return chain.filter(exchange);
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().set("Content-Type", "application/json;charset=UTF-8");
        String payload = "{\"code\":401,\"message\":\"未登录或登录已过期\"}";
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload.getBytes())));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
