package com.fox.urlshortener.link;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RedirectClickFlushSchedulerTest {

    @Mock
    private ShortLinkRedirectCache redirectCache;

    @Mock
    private ShortLinkRepository shortLinkRepository;

    private final Clock clock = Clock.fixed(Instant.parse("2026-06-12T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void flushesRedisClickCountsToDatabase() {
        when(redirectCache.drainClickCounts()).thenReturn(Map.of("aB12xZ", 3L, "zY98wQ", 2L));

        new RedirectClickFlushScheduler(redirectCache, shortLinkRepository, clock)
                .flushClickCounts();

        verify(shortLinkRepository).addClickCount(
                "aB12xZ",
                3,
                Instant.parse("2026-06-12T10:00:00Z"));
        verify(shortLinkRepository).addClickCount(
                "zY98wQ",
                2,
                Instant.parse("2026-06-12T10:00:00Z"));
    }
}
