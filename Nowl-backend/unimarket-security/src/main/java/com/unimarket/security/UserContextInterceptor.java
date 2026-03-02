package com.unimarket.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 负责在请求完成后清理ThreadLocal,防止内存泄漏
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, 
                                @NonNull HttpServletResponse response, 
                                @NonNull Object handler, 
                                Exception ex) {
        // 请求完成后清理ThreadLocal
        UserContextHolder.clear();
    }
}
