package com.fox.urlshortener.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.model.UserRole;
import com.fox.urlshortener.auth.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private UserRepository userRepository;

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void userDetailsServiceLoadsUserByLogin() {
        var user = TestFixtures.user(1L, "fox", UserRole.USER);
        when(userRepository.findByLogin("fox")).thenReturn(Optional.of(user));

        var service = securityConfig.userDetailsService(userRepository);

        assertThat(service.loadUserByUsername("fox")).isSameAs(user);
    }

    @Test
    void userDetailsServiceThrowsWhenUserIsMissing() {
        when(userRepository.findByLogin("missing")).thenReturn(Optional.empty());

        var service = securityConfig.userDetailsService(userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");
    }

    @Test
    void corsConfigurationUsesConfiguredOriginsAndMethods() {
        var source = securityConfig.corsConfigurationSource(TestFixtures.properties());

        CorsConfiguration configuration = source.getCorsConfiguration(
                new MockHttpServletRequest("GET", "/api/v1/links"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("http://localhost:3395");
        assertThat(configuration.getAllowedMethods())
                .containsExactly("GET", "POST", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders())
                .containsExactly("Content-Type", "Authorization");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void authenticationEntryPointReturnsUnauthorizedJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.authenticationEntryPoint()
                .commence(new MockHttpServletRequest("GET", "/api/v1/links"), response,
                        new BadCredentialsException("missing"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Unauthorized\"}");
    }

    @Test
    void accessDeniedHandlerReturnsForbiddenJson() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        securityConfig.accessDeniedHandler()
                .handle(new MockHttpServletRequest("GET", "/api/v1/admin/users"), response,
                        new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).isEqualTo("{\"message\":\"Access denied\"}");
    }

    @Test
    void passwordConfigCreatesBcryptEncoder() {
        PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

        assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(encoder.matches("Password123", encoder.encode("Password123"))).isTrue();
    }
}
