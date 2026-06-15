package com.fox.urlshortener.link;

import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.URL;

public record UpdateShortLinkRequest(
        @URL String originalUrl,
        @Min(1) Integer expiresInDays,
        Boolean active) {
}
