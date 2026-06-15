package com.fox.urlshortener.auth;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.fox.urlshortener.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final AppProperties appProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtTokenService(AppProperties appProperties, Clock clock) {
        this.appProperties = appProperties;
        this.clock = clock;
        this.secretKey = Keys
                .hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(appProperties.jwt().accessExpiration());
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public String username(String token) {
        return claims(token).getSubject();
    }

    public boolean valid(String token, User user) {
        return user.getUsername().equals(username(token))
                && claims(token).getExpiration().after(Date.from(Instant.now(clock)));
    }

    public long accessTokenSeconds() {
        return appProperties.jwt().accessExpiration().toSeconds();
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
