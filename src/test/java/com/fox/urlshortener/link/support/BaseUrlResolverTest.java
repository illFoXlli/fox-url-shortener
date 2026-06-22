package com.fox.urlshortener.link.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.fox.urlshortener.TestFixtures;
import org.junit.jupiter.api.Test;

class BaseUrlResolverTest {

    @Test
    void usesConfiguredShortUrlBaseUrl() {
        BaseUrlResolver resolver = new BaseUrlResolver(TestFixtures.properties());

        assertThat(resolver.resolve()).isEqualTo("http://localhost:3396");
    }
}
