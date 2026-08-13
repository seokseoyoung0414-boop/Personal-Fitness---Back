package com.nody.nodyspring.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtConfig {

    @Value("${jwt.secretkey}")
    private String secretKeyRaw;

    private SecretKey secretKey;

    private static final long EXPIRATION_TIME = 86400000; // 1일

    @PostConstruct
    public void init() {
        // 문자열을 안전한 HMAC 키로 변환 (32바이트 이상 권장)
        this.secretKey = Keys.hmacShaKeyFor(secretKeyRaw.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(String userid) {
        return Jwts.builder()
                .setSubject(userid)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}