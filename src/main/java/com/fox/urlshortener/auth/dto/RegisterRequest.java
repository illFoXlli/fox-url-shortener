package com.fox.urlshortener.auth.dto;

import com.fox.urlshortener.common.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String login,
        @NotBlank @Size(min = 8) @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$", message = ValidationMessages.CREDENTIAL_RULE_MESSAGE) String password) {
}
