package com.fox.urlshortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.User;
import com.fox.urlshortener.auth.UserRole;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceImplTest {

    @Mock
    private ShortLinkRepository repository;

    @Mock
    private ShortCodeGenerator generator;

    @Mock
    private HttpServletRequest request;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);
    private final BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

    @Test
    void createsShortLinkWithUniqueCode() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink saved = TestFixtures.link(10L, user);
        when(generator.generate()).thenReturn("aB12xZ");
        when(repository.existsByCode("aB12xZ")).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenReturn(saved);
        ShortLinkServiceImpl service = service();

        ShortLinkResponse response = service.create(
                new CreateShortLinkRequest("https://example.com", 30),
                user,
                request);

        assertThat(response.code()).isEqualTo("aB12xZ");
        assertThat(response.shortUrl()).isEqualTo("http://localhost:3396/aB12xZ");
    }

    @Test
    void redirectsActiveNonExpiredLinkAndIncrementsCounter() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, user);
        when(repository.findByCode("aB12xZ")).thenReturn(Optional.of(link));

        ShortLink result = service().redirect("aB12xZ");

        assertThat(result.getClickCount()).isEqualTo(1);
    }

    @Test
    void preventsAccessToAnotherUsersLink() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        User other = TestFixtures.user(2L, "other", UserRole.USER);
        when(repository.findById(10L)).thenReturn(Optional.of(TestFixtures.link(10L, owner)));

        assertThatThrownBy(() -> service().get(10L, other, request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void adminCanUpdateAnyLinkStatus() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        User admin = TestFixtures.user(2L, "admin", UserRole.ADMIN);
        ShortLink link = TestFixtures.link(10L, owner);
        when(repository.findById(10L)).thenReturn(Optional.of(link));

        ShortLinkResponse response = service().updateStatus(
                10L,
                new UpdateShortLinkStatusRequest(false),
                admin,
                request);

        assertThat(response.active()).isFalse();
    }

    @Test
    void updatesOwnedLinkFields() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(repository.findById(10L)).thenReturn(Optional.of(link));

        ShortLinkResponse response = service().update(
                10L,
                new UpdateShortLinkRequest("https://new.example.com", 10, true),
                owner,
                request);

        assertThat(response.originalUrl()).isEqualTo("https://new.example.com");
        assertThat(response.expiresAt()).isEqualTo(Instant.parse("2026-06-22T10:00:00Z"));
    }

    @Test
    void softDeleteDisablesOwnedLink() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(repository.findById(10L)).thenReturn(Optional.of(link));

        service().softDelete(10L, owner);

        assertThat(link.isActive()).isFalse();
    }

    @Test
    void hardDeleteRemovesOwnedLink() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(repository.findById(10L)).thenReturn(Optional.of(link));

        service().hardDelete(10L, owner);

        verify(repository).delete(link);
    }

    @Test
    void listsOnlyActiveOwnedLinks() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        when(repository.findAllByUserAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                owner,
                Instant.parse("2026-06-12T10:00:00Z")))
                .thenReturn(List.of(TestFixtures.link(10L, owner)));

        assertThat(service().activeMine(owner, request)).hasSize(1);
    }

    @Test
    void listsAllOwnedLinks() {
        User owner = TestFixtures.user(1L, "owner", UserRole.USER);
        when(repository.findAllByUserOrderByCreatedAtDesc(owner))
                .thenReturn(List.of(TestFixtures.link(10L, owner)));

        assertThat(service().mine(owner, request)).hasSize(1);
    }

    @Test
    void redirectReturnsNotFoundForInactiveLink() {
        User user = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, user);
        link.setActive(false);
        when(repository.findByCode("aB12xZ")).thenReturn(Optional.of(link));

        assertThatThrownBy(() -> service().redirect("aB12xZ"))
                .isInstanceOf(ResponseStatusException.class);
    }

    private ShortLinkServiceImpl service() {
        return new ShortLinkServiceImpl(repository, generator, resolver, TestFixtures.properties(),
                clock);
    }
}
