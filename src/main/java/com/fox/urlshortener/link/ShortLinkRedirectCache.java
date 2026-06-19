package com.fox.urlshortener.link;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

public interface ShortLinkRedirectCache {

    Optional<String> findOriginalUrl(String code);

    void putOriginalUrl(String code, String originalUrl, Duration linkTtl);

    boolean incrementClickCount(String code);

    long drainClickCount(String code);

    Map<String, Long> drainClickCounts();

    void evict(String code);
}
