package com.fox.urlshortener.auth;

public record AuthSession(
        String accessToken,
        String refreshToken,
        long expiresIn,
        CurrentUserResponse user) {
}
