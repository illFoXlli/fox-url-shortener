package com.fox.urlshortener.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.fox.urlshortener.config.AppProperties;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    private final AppProperties appProperties;

    public AuthCookieService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public void addAuthCookies(HttpServletResponse response, AuthSession session) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(appProperties.cookie().accessTokenName(), session.accessToken(),
                        appProperties.jwt().accessExpiration().toSeconds()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(appProperties.cookie().refreshTokenName(), session.refreshToken(),
                        appProperties.jwt().refreshExpiration().toSeconds()).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(appProperties.cookie().accessTokenName(), "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookie(appProperties.cookie().refreshTokenName(), "", 0).toString());
    }

    public String accessToken(HttpServletRequest request) {
        return cookieValue(request, appProperties.cookie().accessTokenName());
    }

    public String refreshToken(HttpServletRequest request) {
        return cookieValue(request, appProperties.cookie().refreshTokenName());
    }

    private ResponseCookie cookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(appProperties.cookie().secure())
                .sameSite(appProperties.cookie().sameSite())
                .path("/")
                .maxAge(maxAgeSeconds);
        if (appProperties.cookie().domain() != null
                && !appProperties.cookie().domain().isBlank()) {
            builder.domain(appProperties.cookie().domain());
        }
        return builder.build();
    }

    private String cookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())
                    && cookie.getValue() != null
                    && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
