package com.limemall.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类。
 * 令牌载荷：sub=用户ID，type=ADMIN/MEMBER（区分后台与商城两端），username=账号。
 * 采用 HS256 对称签名，密钥长度需不小于 32 字节。
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expireMillis;

    public JwtUtil(@Value("${limemall.jwt.secret}") String secret,
                   @Value("${limemall.jwt.expire-hours}") long expireHours) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    /**
     * 生成 JWT 令牌。
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param type     用户类型：ADMIN / MEMBER
     */
    public String createToken(Long userId, String username, String type) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMillis))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验令牌，非法或过期时抛出 JJWT 异常。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
