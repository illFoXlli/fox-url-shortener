package com.fox.urlshortener.auth;

import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest);

    AuthResponse login(LoginRequest request, HttpServletRequest servletRequest);

    AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest);

    void logout(RefreshTokenRequest request);
}
