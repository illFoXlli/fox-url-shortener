package com.fox.urlshortener.link;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateShortLinkRequest(
        @NotBlank @URL String originalUrl,
        @Min(1) Integer expiresInDays) {
}
