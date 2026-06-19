package com.fox.urlshortener.auth.security;

import com.fox.urlshortener.auth.model.User;

public interface JwtTokenService {

    String generateAccessToken(User user);

    String login(String token);

    boolean valid(String token, User user);

    long accessTokenSeconds();
}
