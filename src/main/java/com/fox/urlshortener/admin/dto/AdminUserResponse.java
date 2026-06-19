package com.fox.urlshortener.admin.dto;

import com.fox.urlshortener.auth.model.UserRole;
import java.time.Instant;

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
