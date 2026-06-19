package com.fox.urlshortener.link.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fox.urlshortener.TestFixtures;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BaseUrlResolverTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void usesConfiguredShortUrlBaseUrl() {
        BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

        assertThat(resolver.resolve(request)).isEqualTo("http://localhost:3396");
    }

    @Test
    void ignoresForwardedHeadersWhenBuildingShortUrl() {
        BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

        assertThat(resolver.resolve(request)).isEqualTo("http://localhost:3396");
    }
}
