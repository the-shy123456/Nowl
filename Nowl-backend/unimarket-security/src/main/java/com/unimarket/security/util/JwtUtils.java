package com.unimarket.security.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT工具类，用于生成和验证JWT令牌
 */
@Component
public class JwtUtils {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private Long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 生成JWT令牌 (Access Token)
     * @param claims 自定义声明
     * @return JWT令牌
     */
    public String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     * @param claims 自定义声明
     * @return 刷新令牌
     */
    public String generateRefreshToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .claims(claims)
                .id(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }



    /**
     * 从JWT令牌中获取声明
     * @param token JWT令牌
     * @return 声明
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 验证JWT令牌是否有效
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取JWT令牌中的用户ID
     * @param token JWT令牌
     * @return 用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        } else if (userId instanceof Long) {
            return (Long) userId;
        }

        return null;
    }

    /**
     * 获取JWT令牌中的Subject（通常是用户名或学号）
     * @param token JWT令牌
     * @return Subject
     */
    public String getSubjectFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getSubject();
    }

    /**
     * 获取剩余有效秒数
     * @param token JWT令牌
     * @return 剩余秒数
     */
    public long getRemainingSeconds(String token) {
        Date expiration = getExpirationDate(token);
        if (expiration == null) {
            return 0;
        }
        long seconds = (expiration.getTime() - System.currentTimeMillis()) / 1000;
        return Math.max(0, seconds);
    }

    /**
     * 获取令牌过期时间
     * @param token JWT令牌
     * @return 过期时间
     */
    public Date getExpirationDate(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Access Token有效期（秒）
     */
    public long getAccessExpirationSeconds() {
        if (accessExpiration == null || accessExpiration <= 0) {
            return 0;
        }
        return accessExpiration / 1000;
    }

    /**
     * Refresh Token有效期（秒）
     */
    public long getRefreshExpirationSeconds() {
        if (refreshExpiration == null || refreshExpiration <= 0) {
            return 0;
        }
        return refreshExpiration / 1000;
    }

    /**
     * 获取签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
