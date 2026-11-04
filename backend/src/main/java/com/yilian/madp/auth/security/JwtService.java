package com.yilian.madp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMillis;


    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-hours}") long expirationHours
    )
    {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationHours * 60 * 60 * 1000;
    }

    /**
     * 根据用户 ID 和角色生成 Token。
     */
    public String createToken(Long userId, String role) {
        Date now = new Date();
        Date expiration = new Date(
                now.getTime() + expirationMillis
        );

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 Token 中的用户信息。
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public Long getUserId(String token) {
        return Long.valueOf(
                parseToken(token).getSubject()
        );
    }

    public String getRole(String token) {
        return parseToken(token)
                .get("role", String.class);
    }
}
