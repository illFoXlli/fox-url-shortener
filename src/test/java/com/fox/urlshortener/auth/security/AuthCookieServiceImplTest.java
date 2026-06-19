package com.fox.urlshortener.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.auth.dto.AuthSession;
import com.fox.urlshortener.auth.dto.CurrentUserResponse;
import com.fox.urlshortener.auth.model.UserRole;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthCookieServiceImplTest {

    private final AuthCookieServiceImpl service = new AuthCookieServiceImpl(
            TestFixtures.properties());

    @Test
    void addsHttpOnlyAccessAndRefreshCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthSession session = new AuthSession(
                "access-token",
                "refresh-token",
                900,
                new CurrentUserResponse(1L, "fox", UserRole.USER));

        service.addAuthCookies(response, session);

        List<String> cookies = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(cookies).hasSize(2);
        assertThat(cookies.get(0))
                .contains("fox_access_token=access-token")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        assertThat(cookies.get(1)).contains("fox_refresh_token=refresh-token");
    }

    @Test
    void readsTokensFromRequestCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("fox_access_token", "access-token"),
                new Cookie("fox_refresh_token", "refresh-token"));

        assertThat(service.accessToken(request)).isEqualTo("access-token");
        assertThat(service.refreshToken(request)).isEqualTo("refresh-token");
    }

    @Test
    void clearsAuthCookies() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.clearAuthCookies(response);

        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE))
                .hasSize(2)
                .allMatch(cookie -> cookie.contains("Max-Age=0"));
    }
}
