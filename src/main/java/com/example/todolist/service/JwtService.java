package com.example.todolist.service;

import com.example.todolist.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final Key signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public String generateAccessToken(String subject, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiration = now.plus(Duration.ofMinutes(jwtProperties.accessTokenExpirationMinutes()));
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String subject) {
        Claims claims = parseClaims(token);
        return claims.getSubject().equals(subject) && claims.getExpiration().after(new Date());
    }

    public long getAccessTokenExpirationSeconds() {
        return Duration.ofMinutes(jwtProperties.accessTokenExpirationMinutes()).toSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
