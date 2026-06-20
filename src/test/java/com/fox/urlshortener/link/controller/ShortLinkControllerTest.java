package com.fox.urlshortener.link.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.auth.model.UserRole;
import com.fox.urlshortener.link.dto.CreateShortLinkRequest;
import com.fox.urlshortener.link.dto.ShortLinkResponse;
import com.fox.urlshortener.link.dto.ShortLinkStatsResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkRequest;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import com.fox.urlshortener.link.service.ShortLinkService;
import com.fox.urlshortener.security.CurrentUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class ShortLinkControllerTest {

    @Mock
    private ShortLinkService shortLinkService;

    private ShortLinkController controller;
    private User user;
    private TestingAuthenticationToken authentication;
    private MockHttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        controller = new ShortLinkController(shortLinkService, new CurrentUser());
        user = TestFixtures.user(1L, "fox", UserRole.USER);
        authentication = new TestingAuthenticationToken(user, null);
        servletRequest = new MockHttpServletRequest();
    }

    @Test
    void createDelegatesToServiceWithCurrentUser() {
        CreateShortLinkRequest request = new CreateShortLinkRequest("https://example.com", 30);
        ShortLinkResponse expected = response();
        when(shortLinkService.create(request, user, servletRequest)).thenReturn(expected);

        var result = controller.create(request, authentication, servletRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void mineDelegatesToServiceWithPageable() {
        Pageable pageable = Pageable.ofSize(20);
        Page<ShortLinkResponse> expected = new PageImpl<>(List.of(response()));
        when(shortLinkService.mine(user, servletRequest, pageable)).thenReturn(expected);

        var result = controller.mine(authentication, servletRequest, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void activeMineDelegatesToServiceWithPageable() {
        Pageable pageable = Pageable.ofSize(20);
        Page<ShortLinkResponse> expected = new PageImpl<>(List.of(response()));
        when(shortLinkService.activeMine(user, servletRequest, pageable)).thenReturn(expected);

        var result = controller.activeMine(authentication, servletRequest, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getDelegatesToService() {
        ShortLinkResponse expected = response();
        when(shortLinkService.get(7L, user, servletRequest)).thenReturn(expected);

        var result = controller.get(7L, authentication, servletRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void statsDelegatesToService() {
        ShortLinkStatsResponse expected = new ShortLinkStatsResponse(7L, "aB12xZ", 3, true);
        when(shortLinkService.stats(7L, user)).thenReturn(expected);

        var result = controller.stats(7L, authentication);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void updateDelegatesToService() {
        UpdateShortLinkRequest request = new UpdateShortLinkRequest("https://new.example", 3,
                true);
        ShortLinkResponse expected = response();
        when(shortLinkService.update(7L, request, user, servletRequest)).thenReturn(expected);

        var result = controller.update(7L, request, authentication, servletRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void updateStatusDelegatesToService() {
        UpdateShortLinkStatusRequest request = new UpdateShortLinkStatusRequest(false);
        ShortLinkResponse expected = response();
        when(shortLinkService.updateStatus(7L, request, user, servletRequest))
                .thenReturn(expected);

        var result = controller.updateStatus(7L, request, authentication, servletRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void softDeleteDelegatesToService() {
        controller.softDelete(7L, authentication);

        verify(shortLinkService).softDelete(7L, user);
    }

    private ShortLinkResponse response() {
        Instant now = Instant.parse("2026-06-20T10:15:30Z");
        return new ShortLinkResponse(7L, "aB12xZ", "http://localhost/aB12xZ",
                "https://example.com", true, 0, now, now, now.plusSeconds(3600));
    }
}
