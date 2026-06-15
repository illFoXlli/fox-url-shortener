package com.fox.urlshortener.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.TestFixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository repository;

    @Mock
    private HttpServletRequest request;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void createsHashedRefreshToken() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        when(request.getHeader("User-Agent")).thenReturn("JUnit");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String raw = service().create(user, request);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        String hash = (String) ReflectionTestUtils.getField(captor.getValue(), "tokenHash");
        assertThat(raw).isNotBlank();
        assertThat(hash).hasSize(64).doesNotContain(raw);
    }

    @Test
    void verifiesUsableRefreshToken() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        RefreshToken token = new RefreshToken(
                "hash",
                user,
                Instant.parse("2026-06-12T10:00:00Z"),
                Instant.parse("2026-07-12T10:00:00Z"),
                "JUnit",
                "127.0.0.1");
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThat(service().verify("raw")).isEqualTo(user);
    }

    @Test
    void rejectsExpiredRefreshToken() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        RefreshToken token = new RefreshToken(
                "hash",
                user,
                Instant.parse("2026-05-12T10:00:00Z"),
                Instant.parse("2026-06-01T10:00:00Z"),
                "JUnit",
                "127.0.0.1");
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service().verify("raw"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void revokeMarksExistingToken() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        RefreshToken token = new RefreshToken(
                "hash",
                user,
                Instant.parse("2026-06-12T10:00:00Z"),
                Instant.parse("2026-07-12T10:00:00Z"),
                "JUnit",
                "127.0.0.1");
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service().revoke("raw");

        assertThat(token.getRevokedAt()).isEqualTo(Instant.parse("2026-06-12T10:00:00Z"));
    }

    private RefreshTokenServiceImpl service() {
        return new RefreshTokenServiceImpl(repository, TestFixtures.properties(), clock);
    }
}
