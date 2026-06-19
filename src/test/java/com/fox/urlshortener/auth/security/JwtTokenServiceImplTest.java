package com.fox.urlshortener.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.auth.model.UserRole;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class JwtTokenServiceImplTest {

    private final Clock clock = Clock.systemUTC();
    private final JwtTokenServiceImpl service = new JwtTokenServiceImpl(TestFixtures.properties(),
            clock);

    @Test
    void generatesAndValidatesAccessToken() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);

        String token = service.generateAccessToken(user);

        assertThat(service.login(token)).isEqualTo("fox");
        assertThat(service.valid(token, user)).isTrue();
        assertThat(service.accessTokenSeconds()).isEqualTo(900);
    }
}
