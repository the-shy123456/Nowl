package com.unimarket.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一解析客户端IP，仅在请求来自可信代理时才解析转发头。
 */
@Component
public class HttpRequestIpResolver {

    private final Set<String> trustedProxyIps;

    public HttpRequestIpResolver(@Value("${security.trusted-proxy-ips:127.0.0.1,::1,0:0:0:0:0:0:0:1}") String trustedProxyIpsConfig) {
        this.trustedProxyIps = Arrays.stream(trustedProxyIpsConfig.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::normalizeIp)
                .collect(Collectors.toSet());
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String remoteIp = normalizeIp(request.getRemoteAddr());
        if (!isTrustedProxy(remoteIp)) {
            return remoteIp;
        }

        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String firstHop = forwarded.split(",")[0].trim();
            if (StringUtils.hasText(firstHop)) {
                return normalizeIp(firstHop);
            }
        }

        String realIp = request.getHeader("X-Real-IP");
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
            return ip;
        }
        String normalized = ip.trim();
        if (normalized.startsWith("::ffff:")) {
            return normalized.substring("::ffff:".length());
        }
        return normalized;
    }
}

