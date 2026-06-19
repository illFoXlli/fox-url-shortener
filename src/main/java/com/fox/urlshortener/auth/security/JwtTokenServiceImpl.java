package com.fox.urlshortener.auth.security;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.config.AppProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final AppProperties appProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtTokenServiceImpl(AppProperties appProperties, Clock clock) {
        this.appProperties = appProperties;
        this.clock = clock;
        this.secretKey = Keys
                .hmacShaKeyFor(appProperties.jwt().secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(appProperties.jwt().accessExpiration());
        return Jwts.builder()
                .subject(user.getLogin())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String login(String token) {
        return claims(token).getSubject();
    }

    @Override
    public boolean valid(String token, User user) {
        return user.getLogin().equals(login(token))
                && claims(token).getExpiration().after(Date.from(Instant.now(clock)));
    }

    @Override
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
