package com.fox.urlshortener.auth.dto;

public record AuthSession(
        String accessToken,
        String refreshToken,
        long expiresIn,
        CurrentUserResponse user) {
}
