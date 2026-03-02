package com.unimarket.common.interceptor;

import com.unimarket.security.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 日志拦截器
 * 统一记录请求日志，包括请求路径、方法、用户信息、耗时等
 */
@Slf4j
@Component
public class LogInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "requestStartTime";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        // 记录请求开始时间
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        // 计算请求耗时
        Long startTime = (Long) request.getAttribute(START_TIME);
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;

        // 获取请求信息
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        int status = response.getStatus();

        // 获取用户ID（如果已登录）
        Long userId = null;
        try {
            userId = UserContextHolder.getUserId();
        } catch (Exception contextEx) {
            log.debug("获取用户上下文失败，按匿名请求处理: {}", contextEx.getMessage());
        }

        // 构建日志信息
        String fullPath = queryString != null ? uri + "?<redacted>" : uri;
        String userInfo = resolveUserInfo(userId);

        // 根据状态码和耗时选择日志级别
        if (ex != null) {
            log.error("[{}] {} {} | user={} | status={} | {}ms | error={}",
                    method, fullPath, status, userInfo, status, duration, ex.getMessage());
        } else if (status >= 400) {
            log.warn("[{}] {} | user={} | status={} | {}ms",
                    method, fullPath, userInfo, status, duration);
        } else if (duration > 3000) {
            // 慢请求警告（超过3秒）
            log.warn("[{}] {} | user={} | status={} | {}ms [SLOW]",
                    method, fullPath, userInfo, status, duration);
        } else {
            log.info("[{}] {} | user={} | status={} | {}ms",
                    method, fullPath, userInfo, status, duration);
        }
    }

    private String resolveUserInfo(Long userId) {
        if (userId != null) {
            return String.valueOf(userId);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return "anonymous";
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof com.unimarket.security.CustomUserDetails customUserDetails) {
            return String.valueOf(customUserDetails.getUserId());
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
            return name;
        }
        return "anonymous";
    }
}
