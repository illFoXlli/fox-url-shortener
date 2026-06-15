package com.fox.urlshortener.link;

import jakarta.validation.constraints.NotNull;

public record UpdateShortLinkStatusRequest(@NotNull Boolean active) {
}
