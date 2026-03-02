package com.unimarket.module.user.controller;

import com.unimarket.common.result.Result;
import com.unimarket.module.user.dto.ResetPasswordDTO;
import com.unimarket.module.user.dto.SendSmsDTO;
import com.unimarket.module.user.dto.UserLoginDTO;
import com.unimarket.module.user.dto.UserRegisterDTO;
import com.unimarket.module.user.service.UserService;
import com.unimarket.module.user.vo.AuthSessionVO;
import com.unimarket.module.user.vo.LoginVO;
import com.unimarket.security.UserContextHolder;
import com.unimarket.security.util.JwtUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 认证相关Controller
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    /**
     * 发送短信验证码
     */
    @PostMapping("/send-sms")
    public Result<Void> sendSmsCode(@Valid @RequestBody SendSmsDTO dto) {
        userService.sendSmsCode(dto.getPhone());
        return Result.success();
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<AuthSessionVO> login(@Valid @RequestBody UserLoginDTO dto,
                                       HttpServletRequest request,
                                       HttpServletResponse response) {
        LoginVO loginVO = userService.login(dto);
        boolean secureCookie = isSecureRequest(request);
        long accessMaxAge = resolveAccessCookieMaxAge();
        long refreshMaxAge = resolveRefreshCookieMaxAge();

        // 使用ResponseCookie设置SameSite=Lax（允许页面刷新和跳转时携带Cookie）
        org.springframework.http.ResponseCookie tokenCookie = org.springframework.http.ResponseCookie.from("access_token", loginVO.getToken())
                .httpOnly(true)
                .path("/")
                .maxAge(accessMaxAge)
                .sameSite("Lax")
                .secure(secureCookie)
                .build();

        org.springframework.http.ResponseCookie refreshTokenCookie = org.springframework.http.ResponseCookie.from("refresh_token", loginVO.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(refreshMaxAge)
                .sameSite("Lax")
                .secure(secureCookie)
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, tokenCookie.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // 不在响应体返回可读Token，避免前端落地到localStorage或URL。
        return Result.success(new AuthSessionVO(loginVO.getUserInfo()));
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh-token")
    public Result<AuthSessionVO> refreshToken(@CookieValue(name = "refresh_token", required = false) String refreshToken,
                                              HttpServletRequest request,
                                              HttpServletResponse response) {
        if (refreshToken == null) {
            // 使用特定错误码1008，避免前端响应拦截器再次尝试刷新token导致无限循环
            return Result.error(1008, "刷新令牌已过期或不存在，请重新登录");
        }

        LoginVO loginVO = userService.refreshToken(refreshToken);
        boolean secureCookie = isSecureRequest(request);
        long accessMaxAge = resolveAccessCookieMaxAge();
        long refreshMaxAge = resolveRefreshCookieMaxAge();

        // 更新Access Token Cookie
        org.springframework.http.ResponseCookie tokenCookie = org.springframework.http.ResponseCookie.from("access_token", loginVO.getToken())
                .httpOnly(true)
                .path("/")
                .maxAge(accessMaxAge)
                .sameSite("Lax")
                .secure(secureCookie)
                .build();

        org.springframework.http.ResponseCookie refreshTokenCookie = org.springframework.http.ResponseCookie.from("refresh_token", loginVO.getRefreshToken())
                .httpOnly(true)
                .path("/")
                .maxAge(refreshMaxAge)
                .sameSite("Lax")
                .secure(secureCookie)
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, tokenCookie.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return Result.success(new AuthSessionVO(loginVO.getUserInfo()));
    }
    
    /**
     * 获取图形验证码
     */
    @GetMapping("/captcha")
    public Result<String> getCaptcha(@RequestParam String uuid) {
        return Result.success(userService.getCaptcha(uuid));
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        Long userId = null;
        try {
            userId = UserContextHolder.getUserId();
        } catch (Exception ex) {
            log.debug("登出时未获取到用户上下文，按匿名请求处理: {}", ex.getMessage());
        }
        boolean secureCookie = isSecureRequest(request);
        String accessToken = getCookieValue(request.getCookies(), "access_token");
        String refreshToken = getCookieValue(request.getCookies(), "refresh_token");

        if (userId != null) {
            userService.logout(userId, accessToken, refreshToken);
        }

        // 清除Cookie: access_token
        org.springframework.http.ResponseCookie accessTokenCookie = org.springframework.http.ResponseCookie.from("access_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(secureCookie)
                .build();

        // 清除Cookie: refresh_token
        org.springframework.http.ResponseCookie refreshTokenCookie = org.springframework.http.ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .secure(secureCookie)
                .build();

        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(org.springframework.http.HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        return Result.success();
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-password")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.success();
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

    private String getCookieValue(Cookie[] cookies, String name) {
        if (cookies == null || !StringUtils.hasText(name)) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private long resolveAccessCookieMaxAge() {
        long configured = jwtUtils.getAccessExpirationSeconds();
        return configured > 0 ? configured : 2 * 60 * 60;
    }

    private long resolveRefreshCookieMaxAge() {
        long configured = jwtUtils.getRefreshExpirationSeconds();
        return configured > 0 ? configured : 7 * 24 * 60 * 60;
    }
}

