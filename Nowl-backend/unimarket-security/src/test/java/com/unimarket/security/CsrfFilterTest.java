package com.unimarket.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsrfFilterTest {

    private final CsrfFilter csrfFilter = new CsrfFilter();
    private static final String CSRF_TOKEN = "csrf-token-sample";

    @Test
    @DisplayName("GET请求自动下发CSRF Cookie")
    void issueCsrfCookieForSafeMethod() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/goods");
        request.setScheme("https");
        request.setServerName("app.example.com");
        request.setServerPort(443);

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals(true, response.getHeaders("Set-Cookie").stream().anyMatch(cookie -> cookie.contains("csrf_token=")));
    }

    @Test
    @DisplayName("POST请求缺少X-Requested-With时返回403")
    void rejectWhenMissingRequestedWith() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/order/pay");
        request.setScheme("https");
        request.setServerName("app.example.com");
        request.setServerPort(443);
        request.addHeader("Origin", "https://app.example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("POST请求Origin不可信时返回403")
    void rejectWhenOriginMismatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/order/pay");
        request.setScheme("https");
        request.setServerName("app.example.com");
        request.setServerPort(443);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Origin", "https://evil.example.com");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("POST请求认证态缺少CSRF Token时返回403")
    void rejectWhenAuthenticatedButMissingCsrfToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/order/pay");
        request.setScheme("https");
        request.setServerName("app.example.com");
        request.setServerPort(443);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Origin", "https://app.example.com");
        request.setCookies(new Cookie("access_token", "access-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("POST请求认证态Token不匹配时返回403")
    void rejectWhenCsrfTokenMismatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/order/pay");
        request.setScheme("https");
        request.setServerName("app.example.com");
        request.setServerPort(443);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Origin", "https://app.example.com");
        request.addHeader("X-CSRF-TOKEN", "wrong-token");
        request.setCookies(
                new Cookie("access_token", "access-token"),
                new Cookie("csrf_token", CSRF_TOKEN)
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    @DisplayName("POST请求认证态Token匹配时放行")
    void passWhenCsrfTokenMatches() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/order/pay");
        request.setScheme("https");
        request.setServerName("app.example.com");
        request.setServerPort(443);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Origin", "https://app.example.com");
        request.addHeader("X-CSRF-TOKEN", CSRF_TOKEN);
        request.setCookies(
                new Cookie("access_token", "access-token"),
                new Cookie("csrf_token", CSRF_TOKEN)
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }

    @Test
    @DisplayName("POST请求本地开发跨端口Origin允许放行")
    void passWhenLocalDevCrossOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        request.setScheme("http");
        request.setServerName("127.0.0.1");
        request.setServerPort(8080);
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        request.addHeader("Origin", "http://localhost:5173");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        csrfFilter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
    }
}
