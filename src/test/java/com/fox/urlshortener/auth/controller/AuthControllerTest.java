package com.fox.urlshortener.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.dto.AuthSession;
import com.fox.urlshortener.auth.dto.CurrentUserResponse;
import com.fox.urlshortener.auth.dto.LoginRequest;
import com.fox.urlshortener.auth.dto.RegisterRequest;
import com.fox.urlshortener.auth.model.UserRole;
import com.fox.urlshortener.auth.security.AuthCookieService;
import com.fox.urlshortener.auth.service.AuthService;
import com.fox.urlshortener.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthCookieService authCookieService;

    @Test
    void registerAddsCookiesAndReturnsSessionBody() {
        AuthController controller = controller();
        RegisterRequest registerRequest = new RegisterRequest("fox", "Password123");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        AuthSession session = session();
        when(authService.register(registerRequest, servletRequest)).thenReturn(session);

        var response = controller.register(registerRequest, servletRequest, servletResponse);

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.user().login()).isEqualTo("fox");
        verify(authCookieService).addAuthCookies(servletResponse, session);
    }

    @Test
    void loginAddsCookiesAndReturnsSessionBody() {
        AuthController controller = controller();
        LoginRequest loginRequest = new LoginRequest("fox", "Password123");
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        AuthSession session = session();
        when(authService.login(loginRequest, servletRequest)).thenReturn(session);

        var response = controller.login(loginRequest, servletRequest, servletResponse);

        assertThat(response.accessToken()).isEqualTo("access-token");
        verify(authCookieService).addAuthCookies(servletResponse, session);
    }

    @Test
    void refreshReadsRefreshCookieAndAddsNewAuthCookies() {
        AuthController controller = controller();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        AuthSession session = session();
        when(authCookieService.refreshToken(servletRequest)).thenReturn("refresh-token");
        when(authService.refresh("refresh-token", servletRequest)).thenReturn(session);

        var response = controller.refresh(servletRequest, servletResponse);

        assertThat(response.user().id()).isEqualTo(1L);
        verify(authCookieService).addAuthCookies(servletResponse, session);
    }

    @Test
    void logoutRevokesRefreshCookieAndClearsCookies() {
        AuthController controller = controller();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        MockHttpServletResponse servletResponse = new MockHttpServletResponse();
        when(authCookieService.refreshToken(servletRequest)).thenReturn("refresh-token");

        controller.logout(servletRequest, servletResponse);

        verify(authService).logout("refresh-token");
        verify(authCookieService).clearAuthCookies(servletResponse);
    }

    @Test
    void meReturnsCurrentAuthenticatedUser() {
        AuthController controller = controller();
        var user = TestFixtures.user(1L, "fox", UserRole.USER);

        var response = controller.me(new TestingAuthenticationToken(user, null));

        assertThat(response).isEqualTo(CurrentUserResponse.from(user));
    }

    private AuthController controller() {
        return new AuthController(authService, authCookieService, new CurrentUser());
    }

    private AuthSession session() {
        return new AuthSession("access-token", "refresh-token", 900,
                new CurrentUserResponse(1L, "fox", UserRole.USER));
    }
}
