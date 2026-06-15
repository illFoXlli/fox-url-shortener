package com.fox.urlshortener.bootstrap;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.UserRepository;
import com.fox.urlshortener.config.AppProperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAdminWhenMissing() {
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, passwordEncoder,
                TestFixtures.properties());
        when(userRepository.existsByLogin("admin")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash");

        bootstrap.run(null);

        verify(userRepository).save(any());
    }

    @Test
    void doesNothingWhenAdminExists() {
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, passwordEncoder,
                TestFixtures.properties());
        when(userRepository.existsByLogin("admin")).thenReturn(true);

        bootstrap.run(null);

        verify(userRepository, never()).save(any());
    }

    @Test
    void failsWhenCredentialsAreMissing() {
        AppProperties properties = new AppProperties(
                "http://localhost:3396",
                "http://localhost:3396",
                new AppProperties.Admin("", "", "FoX Admin"),
                TestFixtures.properties().jwt(),
                TestFixtures.properties().cookie(),
                TestFixtures.properties().cors(),
                TestFixtures.properties().shortLink(),
                TestFixtures.properties().forwarded());
        AdminBootstrap bootstrap = new AdminBootstrap(userRepository, passwordEncoder, properties);

        assertThatThrownBy(() -> bootstrap.run(null)).isInstanceOf(IllegalStateException.class);
    }
}
