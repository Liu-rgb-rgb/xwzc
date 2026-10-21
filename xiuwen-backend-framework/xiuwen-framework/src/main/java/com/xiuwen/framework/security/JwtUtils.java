package com.xiuwen.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * JWT 工具类。
 */
@Component
public class JwtUtils {
    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;

    public JwtUtils(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成 Token。
     */
    public String createToken(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new IllegalArgumentException("登录用户信息不能为空");
        }

        Date now = new Date();
        Date expire = new Date(
                now.getTime()
                        + properties.getExpireSeconds() * 1000L
        );

        return Jwts.builder()
                .subject(String.valueOf(loginUser.getUserId()))
                .claim(CLAIM_USERNAME, loginUser.getUsername())
                .claim(CLAIM_ROLE, loginUser.getRole())
                .issuedAt(now)
                .expiration(expire)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成 Token 的便捷方法。
     */
    public String createToken(Long userId,
                              String username,
                              String role) {
        return createToken(
                new LoginUser(userId, username, role)
        );
    }

    /**
     * 解析 Token。
     */
    public LoginUser parseToken(String token) {
        Claims claims = parseClaims(token);

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get(
                CLAIM_USERNAME,
                String.class
        );
        String role = claims.get(
                CLAIM_ROLE,
                String.class
        );

        return new LoginUser(
                userId,
                username,
                role
        );
    }

    /**
     * 判断 Token 是否过期。
     */
    public boolean isExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();

            return expiration == null
                    || expiration.before(new Date());
        } catch (ExpiredJwtException ex) {
            return true;
        }
    }

    /**
     * 解析并校验 Token。
     */
    private Claims parseClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Token 不能为空"
            );
        }

        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }

    /**
     * 根据配置中的密钥生成签名 Key。
     */
    private SecretKey getSigningKey() {
        String secret = properties.getSecret();

        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                    "JWT 密钥不能为空"
            );
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256"
            );
            byte[] keyBytes = digest.digest(
                    secret.trim().getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "无法生成 JWT 签名密钥",
                    ex
            );
        }
    }
}