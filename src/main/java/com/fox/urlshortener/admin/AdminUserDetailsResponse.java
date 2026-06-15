package com.fox.urlshortener.admin;

import java.time.Instant;

import com.fox.urlshortener.auth.UserRole;

public record AdminUserDetailsResponse(
        Long id,
        String username,
        UserRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long linksCount,
        long activeLinksCount,
        long totalClickCount) {
}
