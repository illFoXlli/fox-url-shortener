package com.fox.urlshortener.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.TestFixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseUrlResolverTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void usesConfiguredShortUrlBaseWhenForwardedHeadersAreAbsent() {
        BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

        assertThat(resolver.resolve(request)).isEqualTo("http://localhost:3396");
    }

    @Test
    void usesForwardedHostProtoAndPortWhenPresent() {
        when(request.getHeader("X-Forwarded-Proto")).thenReturn("https");
        when(request.getHeader("X-Forwarded-Host")).thenReturn("api.fox.kh.ua");
        when(request.getHeader("X-Forwarded-Port")).thenReturn("8443");
        BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

        assertThat(resolver.resolve(request)).isEqualTo("https://api.fox.kh.ua:8443");
    }

    @Test
    void usesRequestHostProtoAndPortWhenForwardedHeadersAreAlreadyApplied() {
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("api.fox.kh.ua");
        when(request.getServerPort()).thenReturn(8443);
        BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

        assertThat(resolver.resolve(request)).isEqualTo("https://api.fox.kh.ua:8443");
    }
}
