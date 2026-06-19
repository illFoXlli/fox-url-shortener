package com.fox.urlshortener.link.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateShortLinkStatusRequest(@NotNull Boolean active) {
}
