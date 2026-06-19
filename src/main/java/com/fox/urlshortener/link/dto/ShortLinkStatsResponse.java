package com.fox.urlshortener.link.dto;

public record ShortLinkStatsResponse(Long id, String code, long clickCount, boolean active) {
}
