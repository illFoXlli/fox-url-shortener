package com.fox.urlshortener.admin.dto;

import java.time.Instant;

public record AdminLinkResponse(
        Long id,
        String code,
        String shortUrl,
        String originalUrl,
        boolean active,
        long clickCount,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt,
        Long userId,
        String userLogin) {
}
