package com.fox.urlshortener.link.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.TestFixtures;
import com.fox.urlshortener.config.AppProperties;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RedisShortLinkRedirectCacheTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void returnsCachedOriginalUrl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:aB12xZ:url")).thenReturn("https://example.com");

        Optional<String> result = cache(true).findOriginalUrl("aB12xZ");

        assertThat(result).contains("https://example.com");
    }

    @Test
    void returnsEmptyWhenRedisReadFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("test:aB12xZ:url")).thenThrow(new IllegalStateException("down"));

        Optional<String> result = cache(true).findOriginalUrl("aB12xZ");

        assertThat(result).isEmpty();
    }

    @Test
    void storesOriginalUrlWithConfiguredTtlCap() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache(true).putOriginalUrl("aB12xZ", "https://example.com", Duration.ofMinutes(1));

        verify(valueOperations).set(
                "test:aB12xZ:url",
                "https://example.com",
                Duration.ofSeconds(10));
    }

    @Test
    void skipsExpiredOriginalUrlCacheWrites() {
        cache(true).putOriginalUrl("aB12xZ", "https://example.com", Duration.ZERO);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void incrementsClickCountInRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        boolean incremented = cache(true).incrementClickCount("aB12xZ");

        assertThat(incremented).isTrue();
        verify(valueOperations).increment("test:aB12xZ:clicks");
    }

    @Test
    void returnsFalseWhenRedisClickIncrementFails() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("test:aB12xZ:clicks"))
                .thenThrow(new IllegalStateException("down"));

        assertThat(cache(true).incrementClickCount("aB12xZ")).isFalse();
    }

    @Test
    void drainsClickCountFromRedis() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("test:aB12xZ:clicks")).thenReturn("3");

        assertThat(cache(true).drainClickCount("aB12xZ")).isEqualTo(3);
    }

    @Test
    void evictsRedirectKeys() {
        cache(true).evict("aB12xZ");

        verify(redisTemplate).delete("test:aB12xZ:url");
        verify(redisTemplate).delete("test:aB12xZ:clicks");
    }

    @Test
    void disabledCacheDoesNotUseRedis() {
        RedisShortLinkRedirectCache cache = cache(false);

        assertThat(cache.findOriginalUrl("aB12xZ")).isEmpty();
        cache.putOriginalUrl("aB12xZ", "https://example.com", Duration.ofSeconds(5));
        assertThat(cache.incrementClickCount("aB12xZ")).isFalse();
        assertThat(cache.drainClickCount("aB12xZ")).isZero();
        cache.evict("aB12xZ");

        verify(redisTemplate, never()).opsForValue();
    }

    private RedisShortLinkRedirectCache cache(boolean enabled) {
        return new RedisShortLinkRedirectCache(redisTemplate, properties(enabled));
    }

    private AppProperties properties(boolean enabled) {
        AppProperties properties = TestFixtures.properties();
        return new AppProperties(
                properties.baseUrl(),
                properties.shortUrlBaseUrl(),
                properties.admin(),
                properties.jwt(),
                properties.cookie(),
                properties.cors(),
                properties.shortLink(),
                new AppProperties.RedirectCache(enabled, "test:", 10, 1000),
                properties.forwarded());
    }
}
