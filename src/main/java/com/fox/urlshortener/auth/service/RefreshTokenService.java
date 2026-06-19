package com.fox.urlshortener.auth.service;

import com.fox.urlshortener.auth.model.User;
import jakarta.servlet.http.HttpServletRequest;

public interface RefreshTokenService {

    String create(User user, HttpServletRequest request);

    User verify(String rawToken);

    void revoke(String rawToken);
}
