package com.fox.urlshortener.security;

import com.fox.urlshortener.auth.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public User require(Authentication authentication) {
        return (User) authentication.getPrincipal();
    }
}
