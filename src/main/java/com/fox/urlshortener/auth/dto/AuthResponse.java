package com.fox.urlshortener.auth.dto;

public record AuthResponse(
        String accessToken,
        CurrentUserResponse user) {

    public static AuthResponse from(AuthSession session) {
        return new AuthResponse(session.accessToken(), session.user());
    }
}
