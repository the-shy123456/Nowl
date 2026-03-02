package com.unimarket.gateway.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 网关侧客户端IP解析器：仅信任显式配置的代理地址。
 */
@Slf4j
@Component
public class GatewayClientIpResolver {

    private final Set<String> trustedProxyIps;

    public GatewayClientIpResolver(@Value("${security.trusted-proxy-ips:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String trustedProxyIpsConfig) {
        this.trustedProxyIps = Arrays.stream(trustedProxyIpsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizeIp)
                .collect(Collectors.toSet());
    }

    public String resolve(ServerWebExchange exchange) {
        String remoteIp = "unknown";
        if (exchange != null && exchange.getRequest().getRemoteAddress() != null) {
            remoteIp = normalizeIp(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
        }
        if (!isTrustedProxy(remoteIp)) {
            return remoteIp;
        }

        String xff = exchange == null ? null : exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            String firstHop = xff.split(",")[0].trim();
            if (StringUtils.hasText(firstHop)) {
                return normalizeIp(firstHop);
            }
        }

        String realIp = exchange == null ? null : exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return normalizeIp(realIp.trim());
        }

        return remoteIp;
    }

    private boolean isTrustedProxy(String ip) {
        return StringUtils.hasText(ip) && trustedProxyIps.contains(ip);
    }

    private String normalizeIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "unknown";
        }
        String normalized = ip.trim();
        if (normalized.startsWith("::ffff:")) {
            return normalized.substring("::ffff:".length());
        }
        return normalized;
    }
}

