package com.fox.urlshortener;

import java.time.Instant;

import com.fox.urlshortener.auth.User;
import com.fox.urlshortener.auth.UserRole;
import com.fox.urlshortener.config.AppProperties;
import com.fox.urlshortener.link.ShortLink;

import org.springframework.test.util.ReflectionTestUtils;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static AppProperties properties() {
        return new AppProperties(
                "http://localhost:3396",
                "http://localhost:3396",
                new AppProperties.Admin("admin", "Password123", "FoX Admin"),
                new AppProperties.Jwt("change_me_to_long_secret_change_me_to_long_secret", 15, 30),
                new AppProperties.ShortLink(6, 8, 30),
                new AppProperties.Forwarded(
                        "X-Forwarded-Proto",
                        "X-Forwarded-Host",
                        "X-Forwarded-Port"));
    }

    public static User user(Long id, String username, UserRole role) {
        User user = new User(username, "hash", role);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-06-12T10:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-06-12T10:00:00Z"));
        return user;
    }

    public static ShortLink link(Long id, User user) {
        ShortLink link = new ShortLink(
                "aB12xZ",
                "https://example.com",
                Instant.parse("2026-07-12T10:00:00Z"),
                user);
        ReflectionTestUtils.setField(link, "id", id);
        ReflectionTestUtils.setField(link, "createdAt", Instant.parse("2026-06-12T10:00:00Z"));
        ReflectionTestUtils.setField(link, "updatedAt", Instant.parse("2026-06-12T10:00:00Z"));
        return link;
    }
}
