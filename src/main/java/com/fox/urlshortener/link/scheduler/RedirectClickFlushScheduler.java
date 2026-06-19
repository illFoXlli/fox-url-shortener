package com.fox.urlshortener.link.scheduler;

import com.fox.urlshortener.link.service.ShortLinkRedirectCache;
import com.fox.urlshortener.link.repository.ShortLinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RedirectClickFlushScheduler {

    private final ShortLinkRedirectCache redirectCache;
    private final ShortLinkRepository shortLinkRepository;
    private final Clock clock;

    public RedirectClickFlushScheduler(
            ShortLinkRedirectCache redirectCache,
            ShortLinkRepository shortLinkRepository,
            Clock clock) {
        this.redirectCache = redirectCache;
        this.shortLinkRepository = shortLinkRepository;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.redirect-cache.click-flush-interval-millis}")
    @Transactional
    public void flushClickCounts() {
        Instant now = Instant.now(clock);
        for (Map.Entry<String, Long> entry : redirectCache.drainClickCounts().entrySet()) {
            shortLinkRepository.addClickCount(entry.getKey(), entry.getValue(), now);
        }
    }
}
