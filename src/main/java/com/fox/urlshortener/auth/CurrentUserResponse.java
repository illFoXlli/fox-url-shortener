package com.fox.urlshortener.auth;

public record CurrentUserResponse(Long id, String login, UserRole role) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getLogin(), user.getRole());
    }
}
