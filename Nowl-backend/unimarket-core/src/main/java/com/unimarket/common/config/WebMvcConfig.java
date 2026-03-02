package com.unimarket.common.config;

import com.unimarket.common.interceptor.LogInterceptor;
import com.unimarket.common.interceptor.AdminAuditInterceptor;
import com.unimarket.security.UserContextInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 统一管理拦截器注册
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private LogInterceptor logInterceptor;

    @Autowired
    private AdminAuditInterceptor adminAuditInterceptor;

    @Autowired
    private UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 用户上下文清理拦截器 - 保证最后执行 afterCompletion 时再清理
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/**");

        // 日志拦截器 - 第一个执行，记录所有请求
        registry.addInterceptor(logInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**");

        // 管理后台审计拦截器
        registry.addInterceptor(adminAuditInterceptor)
                .addPathPatterns("/admin/**");
    }
}
