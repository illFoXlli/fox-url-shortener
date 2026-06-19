package com.fox.urlshortener.link;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fox.urlshortener.config.AppProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisShortLinkRedirectCache implements ShortLinkRedirectCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisShortLinkRedirectCache.class);

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public RedisShortLinkRedirectCache(StringRedisTemplate redisTemplate,
            AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    @Override
    public Optional<String> findOriginalUrl(String code) {
        if (!enabled()) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(redisTemplate.opsForValue().get(urlKey(code)));
        } catch (RuntimeException ex) {
            warnFallback(ex);
            return Optional.empty();
        }
    }

    @Override
    public void putOriginalUrl(String code, String originalUrl, Duration linkTtl) {
        if (!enabled()) {
            return;
        }

        Duration ttl = cacheTtl(linkTtl);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(urlKey(code), originalUrl, ttl);
        } catch (RuntimeException ex) {
            warnFallback(ex);
        }
    }

    @Override
    public boolean incrementClickCount(String code) {
        if (!enabled()) {
            return false;
        }

        try {
            redisTemplate.opsForValue().increment(clickKey(code));
            return true;
        } catch (RuntimeException ex) {
            warnFallback(ex);
            return false;
        }
    }

    @Override
    public long drainClickCount(String code) {
        if (!enabled()) {
            return 0;
        }

        try {
            String value = redisTemplate.opsForValue().getAndDelete(clickKey(code));
            return parseCount(value);
        } catch (RuntimeException ex) {
            warnFallback(ex);
            return 0;
        }
    }

    @Override
    public Map<String, Long> drainClickCounts() {
        if (!enabled()) {
            return Map.of();
        }

        try {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (String key : clickKeys().orElseGet(java.util.List::of)) {
                String value = redisTemplate.opsForValue().getAndDelete(key);
                long count = parseCount(value);
                if (count > 0) {
                    counts.put(codeFromClickKey(key), count);
                }
            }
            return counts;
        } catch (RuntimeException ex) {
            warnFallback(ex);
            return Map.of();
        }
    }

    @Override
    public void evict(String code) {
        if (!enabled()) {
            return;
        }

        try {
            redisTemplate.delete(urlKey(code));
            redisTemplate.delete(clickKey(code));
        } catch (RuntimeException ex) {
            warnFallback(ex);
        }
    }

    private boolean enabled() {
        return appProperties.redirectCache().enabled();
    }

    private Duration cacheTtl(Duration linkTtl) {
        Duration configuredTtl = appProperties.redirectCache().ttl();
        if (configuredTtl.isZero() || configuredTtl.isNegative()) {
            return linkTtl;
        }
        return linkTtl.compareTo(configuredTtl) < 0 ? linkTtl : configuredTtl;
    }

    private Optional<Collection<String>> clickKeys() {
        String pattern = keyPrefix() + "*:clicks";
        return Optional.ofNullable(redisTemplate.execute((RedisConnection connection) -> {
            Map<String, Boolean> keys = new LinkedHashMap<>();
            ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
            try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                while (cursor.hasNext()) {
                    keys.put(new String(cursor.next(), StandardCharsets.UTF_8), true);
                }
            }
            return keys.keySet();
        }));
    }

    private long parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Long.parseLong(value);
    }

    private String codeFromClickKey(String key) {
        String prefix = keyPrefix();
        return key.substring(prefix.length(), key.length() - ":clicks".length());
    }

    private String urlKey(String code) {
        return keyPrefix() + code + ":url";
    }

    private String clickKey(String code) {
        return keyPrefix() + code + ":clicks";
    }

    private String keyPrefix() {
        return appProperties.redirectCache().keyPrefix();
    }

    private void warnFallback(RuntimeException ex) {
        LOGGER.warn("Redis redirect cache unavailable, falling back to database: {}",
                ex.getMessage());
    }
}
