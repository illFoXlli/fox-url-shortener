package com.fox.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;

import com.fox.urlshortener.TestFixtures;

import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {

    private final Clock clock = Clock.systemUTC();
    private final JwtTokenService service = new JwtTokenService(TestFixtures.properties(), clock);

    @Test
    void generatesAndValidatesAccessToken() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);

        String token = service.generateAccessToken(user);

        assertThat(service.login(token)).isEqualTo("fox");
        assertThat(service.valid(token, user)).isTrue();
        assertThat(service.accessTokenSeconds()).isEqualTo(900);
    }
}
