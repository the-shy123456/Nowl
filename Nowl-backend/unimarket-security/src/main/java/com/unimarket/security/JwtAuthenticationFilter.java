package com.unimarket.security;

import com.unimarket.security.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器，用于验证请求中的JWT令牌
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 公开接口白名单，这些接口不需要进行JWT解析
     */
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/auth/**",
            "/school/**",
            "/category/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    );

    /**
     * 公开的GET接口白名单
     */
    private static final List<String> PUBLIC_GET_PATHS = Arrays.asList(
            "/goods",
            "/goods/*",
            "/errand/list",
            "/errand/*",
            "/user/*",
            "/user/*/profile",
            "/user/*/following",
            "/user/*/followers",
            "/review/received/*",
            "/review/stats/*",
            "/search/**",
            "/recommend/**"
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // /user/info 必须鉴权，不能被 /user/* 规则放行
        if ("/user/info".equals(path)) {
            return false;
        }

        // WebSocket握手：匿名可继续握手，由端点自行兜底关闭；已登录用户仍走JWT解析注入用户上下文
        if (pathMatcher.match("/ws/**", path)) {
            return !hasToken(request);
        }

        // 检查是否是公开接口
        for (String publicPath : PUBLIC_PATHS) {
            if (pathMatcher.match(publicPath, path)) {
                return true;
            }
        }

        // 公开GET接口：游客可跳过鉴权，已登录用户仍然走过滤器以注入用户上下文
        if ("GET".equalsIgnoreCase(method)) {
            for (String publicGetPath : PUBLIC_GET_PATHS) {
                if (pathMatcher.match(publicGetPath, path)) {
                    return !hasToken(request);
                }
            }
        }

        // OPTIONS预检请求不需要过滤
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        return false;
    }

    private boolean hasToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return true;
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if ("access_token".equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        String studentNo = null;

        // 1. 尝试从Authorization头获取Bearer令牌
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
        } else {
            // 2. 尝试从Cookie中获取token (HttpOnly方案)
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("access_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }

        if (token != null) {
            try {
                // 检查黑名单
                String blacklistKey = "token:blacklist:" + token;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                    logger.warn("检测到黑名单Token请求: " + maskToken(token));
                } else {
                    studentNo = jwtUtils.getSubjectFromToken(token);
                }
            } catch (Exception e) {
                // JWT令牌无效
                logger.error("JWT token validation failed: " + e.getMessage());
            }
        }


        // 如果找到了studentNo且SecurityContext中没有认证信息,则进行认证
        if (studentNo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(studentNo);

            // 验证令牌有效性
            if (jwtUtils.validateToken(token)) {
                // 创建认证令牌
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证令牌设置到SecurityContext中
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                // 构建完整的用户上下文并存储到ThreadLocal
                UserContext userContext = UserContext.builder()
                        .userId(userDetails.getUserId())
                        .studentNo(studentNo)
                        .schoolCode(userDetails.getSchoolCode())
                        .campusCode(userDetails.getCampusCode())
                        .authStatus(userDetails.getAuthStatus())
                        .build();
                UserContextHolder.setContext(userContext);
            }
        }

        // 继续过滤链
        // ThreadLocal的清理由UserContextInterceptor的afterCompletion方法负责
        filterChain.doFilter(request, response);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        String prefix = token.substring(0, 4);
        String suffix = token.substring(token.length() - 4);
        return prefix + "..." + suffix;
    }
}


