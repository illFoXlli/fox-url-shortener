package com.fox.urlshortener.auth;

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
    public AuthResponse register(RegisterRequest request, HttpServletRequest servletRequest) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        User user = userRepository.save(new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                UserRole.USER));
        return response(user, servletRequest);
    }

    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest servletRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()));
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid credentials"));
        return response(user, servletRequest);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request, HttpServletRequest servletRequest) {
        User user = refreshTokenService.verify(request.refreshToken());
        return response(user, servletRequest);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse response(User user, HttpServletRequest request) {
        return new AuthResponse(
                jwtTokenService.generateAccessToken(user),
                refreshTokenService.create(user, request),
                "Bearer",
                jwtTokenService.accessTokenSeconds());
    }
}
