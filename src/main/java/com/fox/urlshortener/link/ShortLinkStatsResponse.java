package com.fox.urlshortener.link;

public record ShortLinkStatsResponse(Long id, String code, long clickCount, boolean active) {
}
