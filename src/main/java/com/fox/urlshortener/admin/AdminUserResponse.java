package com.fox.urlshortener.admin;

import java.time.Instant;

import com.fox.urlshortener.auth.UserRole;

public record AdminUserResponse(
        Long id,
        String login,
        UserRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt,
        long linksCount,
        long activeLinksCount) {
}
