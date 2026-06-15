package com.fox.urlshortener.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.config.AppProperties;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;
    private final Clock clock;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            AppProperties appProperties,
            Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public String create(User user, HttpServletRequest request) {
        String rawToken = UUID.randomUUID().toString() + UUID.randomUUID();
        Instant now = Instant.now(clock);
        refreshTokenRepository.save(new RefreshToken(
                hash(rawToken),
                user,
                now,
                now.plus(appProperties.jwt().refreshExpiration()),
                request.getHeader("User-Agent"),
                request.getRemoteAddr()));
        return rawToken;
    }

    @Override
    @Transactional
    public User verify(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid refresh token"));
        if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(Instant.now(clock))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        return token.getUser();
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> token.revoke(Instant.now(clock)));
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
