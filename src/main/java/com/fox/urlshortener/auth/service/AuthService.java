package com.fox.urlshortener.auth.service;

import com.fox.urlshortener.auth.dto.AuthSession;
import com.fox.urlshortener.auth.dto.LoginRequest;
import com.fox.urlshortener.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthSession register(RegisterRequest request, HttpServletRequest servletRequest);

    AuthSession login(LoginRequest request, HttpServletRequest servletRequest);

    AuthSession refresh(String refreshToken, HttpServletRequest servletRequest);

    void logout(String refreshToken);
}
