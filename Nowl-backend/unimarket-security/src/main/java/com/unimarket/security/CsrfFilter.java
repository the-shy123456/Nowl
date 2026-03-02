package com.unimarket.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * CSRF防护过滤器。
 * 对认证态写请求启用双提交Token校验（Cookie + Header），并叠加Origin/Referer来源校验。
 */
@Component
public class CsrfFilter extends OncePerRequestFilter {

    private static final String REQUESTED_WITH_HEADER = "X-Requested-With";
    private static final String REQUESTED_WITH_VALUE = "XMLHttpRequest";
    private static final String CSRF_COOKIE_NAME = "csrf_token";
    private static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final long CSRF_COOKIE_MAX_AGE_SECONDS = 12 * 60 * 60;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 允许的来源列表（逗号分隔），用于前后端分离部署时放行前端域名。
     * 例如：security.csrf.allowed-origins=http://localhost:5173,https://app.example.com
     */
    @Value("${security.csrf.allowed-origins:}")
    private String allowedOriginsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        ensureCsrfCookie(request, response);

        String method = request.getMethod();
        // GET/OPTIONS/HEAD 等幂等请求通常不需要CSRF防护
        if ("GET".equals(method) || "OPTIONS".equals(method) || "HEAD".equals(method) || "TRACE".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestedWith = request.getHeader(REQUESTED_WITH_HEADER);
        if (!REQUESTED_WITH_VALUE.equals(requestedWith)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF校验失败：非法请求来源\"}");
            return;
        }

        String expectedOrigin = resolveExpectedOrigin(request);
        String origin = request.getHeader("Origin");
        if (StringUtils.hasText(origin) && !isOriginAllowed(origin, expectedOrigin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF校验失败：Origin不可信\"}");
            return;
        }

        String referer = request.getHeader("Referer");
        if (!StringUtils.hasText(origin) && StringUtils.hasText(referer) && !isRefererAllowed(referer, expectedOrigin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF校验失败：Referer不可信\"}");
            return;
        }

        if (hasAuthSessionCookie(request) && !isCsrfTokenValid(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":403,\"message\":\"CSRF校验失败：Token无效\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void ensureCsrfCookie(HttpServletRequest request, HttpServletResponse response) {
        if (StringUtils.hasText(getCookieValue(request, CSRF_COOKIE_NAME))) {
            return;
        }

        ResponseCookie csrfCookie = ResponseCookie.from(CSRF_COOKIE_NAME, generateCsrfToken())
                .httpOnly(false)
                .path("/")
                .maxAge(CSRF_COOKIE_MAX_AGE_SECONDS)
                .sameSite("Lax")
                .secure(isSecureRequest(request))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }

    private boolean hasAuthSessionCookie(HttpServletRequest request) {
        return StringUtils.hasText(getCookieValue(request, ACCESS_TOKEN_COOKIE))
                || StringUtils.hasText(getCookieValue(request, REFRESH_TOKEN_COOKIE));
    }

    private boolean isCsrfTokenValid(HttpServletRequest request) {
        String cookieToken = getCookieValue(request, CSRF_COOKIE_NAME);
        String headerToken = request.getHeader(CSRF_HEADER_NAME);
        if (!StringUtils.hasText(cookieToken) || !StringUtils.hasText(headerToken)) {
            return false;
        }
        return MessageDigest.isEqual(
                cookieToken.getBytes(StandardCharsets.UTF_8),
                headerToken.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        if (!StringUtils.hasText(cookieName)) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String generateCsrfToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (!StringUtils.hasText(forwardedProto)) {
            return false;
        }
        String proto = forwardedProto.split(",")[0].trim();
        return "https".equalsIgnoreCase(proto);
    }

    private String resolveExpectedOrigin(HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (!StringUtils.hasText(scheme)) {
            scheme = request.getScheme();
        } else {
            scheme = scheme.split(",")[0].trim();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (!StringUtils.hasText(host)) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean useDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);
            host = formatHostWithPort(host, port, scheme, useDefaultPort);
        } else {
            host = host.split(",")[0].trim();
            host = normalizeForwardedHost(host);
        }

        return scheme + "://" + host;
    }

    private String formatHostWithPort(String host, int port, String scheme, boolean useDefaultPort) {
        String formattedHost = host;
        if (StringUtils.hasText(formattedHost) && formattedHost.contains(":") && !formattedHost.startsWith("[")) {
            formattedHost = "[" + formattedHost + "]";
        }
        if (!useDefaultPort && port > 0) {
            formattedHost = formattedHost + ":" + port;
        }
        return formattedHost;
    }

    private String normalizeForwardedHost(String forwardedHost) {
        if (!StringUtils.hasText(forwardedHost)) {
            return forwardedHost;
        }

        String host = forwardedHost.trim();
        if (host.startsWith("[") && host.contains("]")) {
            return host;
        }

        // 若为未加[]的IPv6（可能带端口），尝试规范化为RFC格式：[ipv6]:port
        long colonCount = host.chars().filter(ch -> ch == ':').count();
        if (colonCount <= 1) {
            return host;
        }

        int lastColon = host.lastIndexOf(':');
        if (lastColon <= 0 || lastColon == host.length() - 1) {
            return "[" + host + "]";
        }

        String lastPart = host.substring(lastColon + 1);
        if (lastPart.matches("\\d{1,5}")) {
            try {
                int port = Integer.parseInt(lastPart);
                if (port >= 0 && port <= 65535) {
                    String ipv6 = host.substring(0, lastColon);
                    return "[" + ipv6 + "]:" + port;
                }
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }

        return "[" + host + "]";
    }

    private boolean isOriginAllowed(String origin, String expectedOrigin) {
        String normalizedOrigin = normalizeOrigin(origin);
        String normalizedExpected = normalizeOrigin(expectedOrigin);
        if (!StringUtils.hasText(normalizedOrigin) || !StringUtils.hasText(normalizedExpected)) {
            return false;
        }

        if (normalizedOrigin.equalsIgnoreCase(normalizedExpected)) {
            return true;
        }

        // 本地开发：允许 localhost/127.0.0.1/::1 跨端口访问（前端 dev server、网关、后端端口不同）
        if (isLocalExpectedOrigin(normalizedExpected) && isLocalOrigin(normalizedOrigin)) {
            return true;
        }

        return isExplicitlyAllowedOrigin(normalizedOrigin);
    }

    private boolean isRefererAllowed(String referer, String expectedOrigin) {
        String refererOrigin = extractOriginFromUrl(referer);
        if (!StringUtils.hasText(refererOrigin)) {
            return false;
        }
        return isOriginAllowed(refererOrigin, expectedOrigin);
    }

    private boolean isExplicitlyAllowedOrigin(String origin) {
        if (!StringUtils.hasText(allowedOriginsConfig)) {
            return false;
        }

        String normalizedOrigin = normalizeOrigin(origin);
        for (String rawAllowed : allowedOriginsConfig.split(",")) {
            String allowed = normalizeOrigin(rawAllowed);
            if (!StringUtils.hasText(allowed)) {
                continue;
            }
            if (normalizedOrigin.equalsIgnoreCase(allowed)) {
                return true;
            }
        }
        return false;
    }

    private String extractOriginFromUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (!StringUtils.hasText(scheme) || !StringUtils.hasText(host)) {
                return null;
            }

            String originHost = host.contains(":") ? "[" + host + "]" : host;
            int port = uri.getPort();
            boolean useDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                    || ("https".equalsIgnoreCase(scheme) && port == 443);

            String origin = scheme + "://" + originHost;
            if (port != -1 && !useDefaultPort) {
                origin = origin + ":" + port;
            }
            return origin;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return "";
        }
        String normalized = origin.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isLocalExpectedOrigin(String expectedOrigin) {
        return isLocalHost(extractHostFromOrigin(expectedOrigin));
    }

    private boolean isLocalOrigin(String origin) {
        return isLocalHost(extractHostFromOrigin(origin));
    }

    private String extractHostFromOrigin(String origin) {
        if (!StringUtils.hasText(origin)) {
            return null;
        }
        try {
            return URI.create(origin.trim()).getHost();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isLocalHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }
        String normalized = host.trim();
        return "localhost".equalsIgnoreCase(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equalsIgnoreCase(normalized)
                || "0:0:0:0:0:0:0:1".equalsIgnoreCase(normalized);
    }
}
