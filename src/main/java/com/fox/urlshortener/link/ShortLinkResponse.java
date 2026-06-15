package com.fox.urlshortener.link;

import java.time.Instant;

public record ShortLinkResponse(
        Long id,
        String code,
        String shortUrl,
        String originalUrl,
        boolean active,
        long clickCount,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {
}
