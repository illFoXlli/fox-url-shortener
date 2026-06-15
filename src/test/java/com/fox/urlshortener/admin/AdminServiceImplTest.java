package com.fox.urlshortener.admin;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.fox.urlshortener.auth.UserRepository;
import com.fox.urlshortener.auth.UserRole;
import com.fox.urlshortener.link.BaseUrlResolver;
import com.fox.urlshortener.link.ShortLink;
import com.fox.urlshortener.link.ShortLinkRepository;
import com.fox.urlshortener.link.UpdateShortLinkStatusRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShortLinkRepository linkRepository;

    @Mock
    private HttpServletRequest request;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);
    private final BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

    @Test
    void returnsUsersWithLinkCounts() {
        User admin = TestFixtures.user(1L, "admin", UserRole.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(admin));
        when(linkRepository.countByUser(admin)).thenReturn(2L);
        when(linkRepository.countByUserAndActiveTrueAndExpiresAtAfter(
                admin,
                Instant.parse("2026-06-12T10:00:00Z"))).thenReturn(1L);

        List<AdminUserResponse> users = service().users();

        assertThat(users).hasSize(1);
        assertThat(users.getFirst().activeLinksCount()).isEqualTo(1);
    }

    @Test
    void updatesAnyLinkStatus() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(linkRepository.findById(10L)).thenReturn(Optional.of(link));

        AdminLinkResponse response = service().updateStatus(
                10L,
                new UpdateShortLinkStatusRequest(false),
                request);

        assertThat(response.active()).isFalse();
    }

    @Test
    void returnsUserDetailsWithTotalClicks() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        link.incrementClickCount();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(linkRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(link));
        when(linkRepository.countByUser(owner)).thenReturn(1L);

        AdminUserDetailsResponse response = service().user(1L);

        assertThat(response.totalClickCount()).isEqualTo(1);
        assertThat(response.linksCount()).isEqualTo(1);
    }

    @Test
    void searchesLinksWithFilters() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(linkRepository.search(true, false, "fox", Instant.parse("2026-06-12T10:00:00Z")))
                .thenReturn(List.of(link));

        List<AdminLinkResponse> links = service().links(true, false, "fox", request);

        assertThat(links).hasSize(1);
        assertThat(links.getFirst().shortUrl()).isEqualTo("http://localhost:3396/aB12xZ");
    }

    @Test
    void returnsLinksForUser() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(linkRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(TestFixtures.link(10L, owner)));

        assertThat(service().userLinks(1L, request)).hasSize(1);
    }

    @Test
    void returnsActiveLinksForUser() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(linkRepository.findAllByUserIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                1L,
                Instant.parse("2026-06-12T10:00:00Z")))
                .thenReturn(List.of(TestFixtures.link(10L, owner)));

        assertThat(service().activeUserLinks(1L, request)).hasSize(1);
    }

    @Test
    void softDeletesAnyLink() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(linkRepository.findById(10L)).thenReturn(Optional.of(link));

        service().softDelete(10L);

        assertThat(link.isActive()).isFalse();
    }

    @Test
    void hardDeletesLink() {
        User owner = TestFixtures.user(1L, "fox", UserRole.USER);
        ShortLink link = TestFixtures.link(10L, owner);
        when(linkRepository.findById(10L)).thenReturn(Optional.of(link));

        service().hardDelete(10L);

        verify(linkRepository).delete(link);
    }

    private AdminServiceImpl service() {
        return new AdminServiceImpl(userRepository, linkRepository, resolver, clock);
    }
}
