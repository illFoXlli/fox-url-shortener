package com.fox.urlshortener.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl,
        String shortUrlBaseUrl,
        Admin admin,
        Jwt jwt,
        ShortLink shortLink,
        Forwarded forwarded) {

    public record Admin(String username, String password, String displayName) {
    }

    public record Jwt(String secret, long accessExpirationMinutes, long refreshExpirationDays) {

        public Duration accessExpiration() {
            return Duration.ofMinutes(accessExpirationMinutes);
        }

        public Duration refreshExpiration() {
            return Duration.ofDays(refreshExpirationDays);
        }
    }

    public record ShortLink(int codeMinLength, int codeMaxLength, int defaultExpirationDays) {
    }

    public record Forwarded(String protoHeader, String hostHeader, String portHeader) {
    }
}
