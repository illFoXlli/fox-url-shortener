package com.fox.urlshortener.auth.service;

import com.fox.urlshortener.auth.dto.AuthSession;
import com.fox.urlshortener.auth.dto.CurrentUserResponse;
import com.fox.urlshortener.auth.dto.LoginRequest;
import com.fox.urlshortener.auth.dto.RegisterRequest;
import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.auth.repository.UserRepository;
import com.fox.urlshortener.auth.model.UserRole;
import com.fox.urlshortener.auth.security.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthSession register(RegisterRequest request, HttpServletRequest servletRequest) {
        if (userRepository.existsByLogin(request.login())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login already exists");
        }
        User user = userRepository.save(new User(
                request.login(),
                passwordEncoder.encode(request.password()),
                UserRole.USER));
        return response(user, servletRequest);
    }

    @Override
    public AuthSession login(LoginRequest request, HttpServletRequest servletRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.login(),
                request.password()));
        User user = userRepository.findByLogin(request.login())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"));
        return response(user, servletRequest);
    }

    @Override
    @Transactional
    public AuthSession refresh(String refreshToken, HttpServletRequest servletRequest) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
        }
        User user = refreshTokenService.verify(refreshToken);
        return response(user, servletRequest);
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    private AuthSession response(User user, HttpServletRequest request) {
        return new AuthSession(
                jwtTokenService.generateAccessToken(user),
                refreshTokenService.create(user, request),
                jwtTokenService.accessTokenSeconds(),
                CurrentUserResponse.from(user));
    }
}
