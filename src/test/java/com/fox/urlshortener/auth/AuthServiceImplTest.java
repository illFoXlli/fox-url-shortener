package com.fox.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.TestFixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private HttpServletRequest servletRequest;

    @InjectMocks
    private AuthServiceImpl service;

    @Test
    void registerCreatesUserAndReturnsSession() {
        User saved = TestFixtures.user(1L, "fox", UserRole.USER);
        when(passwordEncoder.encode("Password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(jwtTokenService.generateAccessToken(saved)).thenReturn("access");
        when(jwtTokenService.accessTokenSeconds()).thenReturn(900L);
        when(refreshTokenService.create(saved, servletRequest)).thenReturn("refresh");

        AuthSession response = service.register(new RegisterRequest("fox", "Password123"),
                servletRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");
        assertThat(response.user().login()).isEqualTo("fox");
    }

    @Test
    void registerRejectsDuplicateLogin() {
        RegisterRequest request = new RegisterRequest("fox", "Password123");
        when(userRepository.existsByLogin("fox")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request, servletRequest))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void loginAuthenticatesAndReturnsSession() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        when(userRepository.findByLogin("fox")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(user)).thenReturn("access");
        when(jwtTokenService.accessTokenSeconds()).thenReturn(900L);
        when(refreshTokenService.create(user, servletRequest)).thenReturn("refresh");

        AuthSession response = service.login(new LoginRequest("fox", "Password123"),
                servletRequest);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void refreshVerifiesRefreshTokenAndReturnsNewPair() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        when(refreshTokenService.verify("old-refresh")).thenReturn(user);
        when(jwtTokenService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtTokenService.accessTokenSeconds()).thenReturn(900L);
        when(refreshTokenService.create(user, servletRequest)).thenReturn("new-refresh");

        AuthSession response = service.refresh("old-refresh", servletRequest);

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void logoutRevokesRefreshToken() {
        service.logout("refresh");

        verify(refreshTokenService).revoke("refresh");
    }
}
