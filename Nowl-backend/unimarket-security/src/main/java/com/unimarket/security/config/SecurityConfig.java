package com.unimarket.security.config;

import com.unimarket.security.CsrfFilter;
import com.unimarket.security.JwtAuthenticationFilter;
import com.unimarket.security.handler.RestAuthenticationEntryPoint;
import com.unimarket.security.handler.RestfulAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security配置类
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private RestfulAccessDeniedHandler restfulAccessDeniedHandler;
    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private CsrfFilter csrfFilter;

    /**
     * 密码编码器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的域名
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("http://127.0.0.1:*");

        // 允许携带凭证（Cookie）
        configuration.setAllowCredentials(true);

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));

        // 预检请求有效期
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 安全过滤器链配置
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 配置CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 禁用CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 禁用表单登录
                .formLogin(AbstractHttpConfigurer::disable)

                // 禁用HTTP Basic认证
                .httpBasic(AbstractHttpConfigurer::disable)

                // 配置会话管理为无状态（使用JWT）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 需要登录的公开前缀子路由（需在permitAll规则前声明）
                        .requestMatchers(HttpMethod.GET, "/goods/my", "/goods/collections", "/user/info").authenticated()
                        // 允许匿名访问的接口
                        .requestMatchers("/auth/**", "/school/**", "/category/**").permitAll()
                        // 允许匿名访问的公开浏览接口（GET请求）
                        .requestMatchers(HttpMethod.GET, "/goods", "/goods/{id}").permitAll()  // 商品列表和详情
                        .requestMatchers(HttpMethod.GET, "/errand/list", "/errand/{taskId}").permitAll()  // 跑腿列表和详情
                        .requestMatchers(HttpMethod.GET, "/user/{userId}").permitAll()  // 用户信息
                        .requestMatchers(HttpMethod.GET, "/user/{userId}/following", "/user/{userId}/followers").permitAll()  // 关注/粉丝列表
                        .requestMatchers(HttpMethod.GET, "/review/received/{userId}", "/review/stats/{userId}").permitAll()  // 用户评价
                        .requestMatchers(HttpMethod.GET, "/search/**").permitAll()  // 搜索接口
                        .requestMatchers(HttpMethod.GET, "/recommend/**").permitAll()  // 推荐接口
                        // 允许静态资源访问
                        .requestMatchers(HttpMethod.GET, "/", "/*.html", "/**/*.html", "/**/*.css", "/**/*.js", "/profile/**").permitAll()
                        // 允许Swagger相关访问
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()
                        // 允许跨域预检请求
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                );

        // 自定义权限拒绝处理
        http.exceptionHandling(exception -> exception
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint));

        // 添加CSRF和JWT过滤器
        http.addFilterBefore(csrfFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
