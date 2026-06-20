package com.fox.urlshortener.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.admin.dto.AdminLinkResponse;
import com.fox.urlshortener.admin.dto.AdminUserDetailsResponse;
import com.fox.urlshortener.admin.dto.AdminUserResponse;
import com.fox.urlshortener.admin.service.AdminService;
import com.fox.urlshortener.auth.model.UserRole;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
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

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private AdminController controller;
    private MockHttpServletRequest servletRequest;

    @BeforeEach
    void setUp() {
        controller = new AdminController(adminService);
        servletRequest = new MockHttpServletRequest();
    }

    @Test
    void usersDelegatesToServiceWithPageable() {
        Pageable pageable = Pageable.ofSize(20);
        Page<AdminUserResponse> expected = new PageImpl<>(List.of(userResponse()));
        when(adminService.users(pageable)).thenReturn(expected);

        var result = controller.users(pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void userDelegatesToService() {
        AdminUserDetailsResponse expected = userDetailsResponse();
        when(adminService.user(1L)).thenReturn(expected);

        var result = controller.user(1L);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void linksDelegatesToServiceWithFilters() {
        Pageable pageable = Pageable.ofSize(20);
        Page<AdminLinkResponse> expected = new PageImpl<>(List.of(linkResponse()));
        when(adminService.links(true, false, "fox", servletRequest, pageable))
                .thenReturn(expected);

        var result = controller.links(true, false, "fox", servletRequest, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void userLinksDelegatesToService() {
        Pageable pageable = Pageable.ofSize(20);
        Page<AdminLinkResponse> expected = new PageImpl<>(List.of(linkResponse()));
        when(adminService.userLinks(1L, servletRequest, pageable)).thenReturn(expected);

        var result = controller.userLinks(1L, servletRequest, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void activeUserLinksDelegatesToService() {
        Pageable pageable = Pageable.ofSize(20);
        Page<AdminLinkResponse> expected = new PageImpl<>(List.of(linkResponse()));
        when(adminService.activeUserLinks(1L, servletRequest, pageable)).thenReturn(expected);

        var result = controller.activeUserLinks(1L, servletRequest, pageable);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void linkDelegatesToService() {
        AdminLinkResponse expected = linkResponse();
        when(adminService.link(7L, servletRequest)).thenReturn(expected);

        var result = controller.link(7L, servletRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void updateStatusDelegatesToService() {
        UpdateShortLinkStatusRequest request = new UpdateShortLinkStatusRequest(false);
        AdminLinkResponse expected = linkResponse();
        when(adminService.updateStatus(7L, request, servletRequest)).thenReturn(expected);

        var result = controller.updateStatus(7L, request, servletRequest);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void softDeleteDelegatesToService() {
        controller.softDelete(7L);

        verify(adminService).softDelete(7L);
    }

    @Test
    void hardDeleteDelegatesToService() {
        controller.hardDelete(7L);

        verify(adminService).hardDelete(7L);
    }

    private AdminUserResponse userResponse() {
        Instant now = Instant.parse("2026-06-20T10:15:30Z");
        return new AdminUserResponse(1L, "fox", UserRole.USER, true, now, now, 2, 1);
    }

    private AdminUserDetailsResponse userDetailsResponse() {
        Instant now = Instant.parse("2026-06-20T10:15:30Z");
        return new AdminUserDetailsResponse(1L, "fox", UserRole.USER, true, now, now, 2, 1,
                12);
    }

    private AdminLinkResponse linkResponse() {
        Instant now = Instant.parse("2026-06-20T10:15:30Z");
        return new AdminLinkResponse(7L, "aB12xZ", "http://localhost/aB12xZ",
                "https://example.com", true, 0, now, now, now.plusSeconds(3600), 1L,
                "fox");
    }
}
