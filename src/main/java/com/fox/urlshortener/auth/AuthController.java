package com.fox.urlshortener.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.fox.urlshortener.security.CurrentUser;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookieService authCookieService;
    private final CurrentUser currentUser;

    public AuthController(
            AuthService authService,
            AuthCookieService authCookieService,
            CurrentUser currentUser) {
        this.authService = authService;
        this.authCookieService = authCookieService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthResponse register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthSession session = authService.register(request, servletRequest);
        authCookieService.addAuthCookies(servletResponse, session);
        return new AuthResponse(session.user());
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthSession session = authService.login(request, servletRequest);
        authCookieService.addAuthCookies(servletResponse, session);
        return new AuthResponse(session.user());
    }

    @PostMapping("/refresh")
    AuthResponse refresh(HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        AuthSession session = authService.refresh(authCookieService.refreshToken(servletRequest),
                servletRequest);
        authCookieService.addAuthCookies(servletResponse, session);
        return new AuthResponse(session.user());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        authService.logout(authCookieService.refreshToken(servletRequest));
        authCookieService.clearAuthCookies(servletResponse);
    }

    @GetMapping("/me")
    CurrentUserResponse me(Authentication authentication) {
        return CurrentUserResponse.from(currentUser.require(authentication));
    }
}
