package com.fox.urlshortener.auth.dto;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.auth.model.UserRole;

public record CurrentUserResponse(Long id, String login, UserRole role) {

    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getId(), user.getLogin(), user.getRole());
    }
}
