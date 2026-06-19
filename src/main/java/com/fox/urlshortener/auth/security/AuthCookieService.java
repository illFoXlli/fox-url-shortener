package com.fox.urlshortener.auth.security;

import com.fox.urlshortener.auth.dto.AuthSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthCookieService {

    void addAuthCookies(HttpServletResponse response, AuthSession session);

    void clearAuthCookies(HttpServletResponse response);

    String accessToken(HttpServletRequest request);

    String refreshToken(HttpServletRequest request);
}
