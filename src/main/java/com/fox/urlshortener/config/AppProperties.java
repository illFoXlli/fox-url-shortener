package com.fox.urlshortener.config;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String shortUrlBaseUrl,
        Admin admin,
        Jwt jwt,
        Cookie cookie,
        Cors cors,
        ShortLink shortLink,
        Forwarded forwarded) {

    public record Admin(String login, String password, String displayName) {
    }

    public record Jwt(String secret, long accessExpirationMinutes, long refreshExpirationDays) {

        public Duration accessExpiration() {
            return Duration.ofMinutes(accessExpirationMinutes);
        }

        public Duration refreshExpiration() {
            return Duration.ofDays(refreshExpirationDays);
        }
    }

    public record ShortLink(
            int codeMinLength,
            int codeMaxLength,
            int defaultExpirationDays,
            long redirectCacheMaxAgeSeconds) {
    }

    public record Cookie(
            String accessTokenName,
            String refreshTokenName,
            boolean secure,
            String sameSite,
            String domain) {
    }

    public record Cors(String allowedOrigins) {

        public List<String> allowedOriginList() {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                return List.of();
            }
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(origin -> !origin.isBlank())
                    .toList();
        }
    }

    public record Forwarded(String protoHeader, String hostHeader, String portHeader) {
    }
}
