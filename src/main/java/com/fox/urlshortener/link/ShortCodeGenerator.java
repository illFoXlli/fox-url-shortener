package com.fox.urlshortener.link;

import java.security.SecureRandom;

import com.fox.urlshortener.config.AppProperties;

import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom secureRandom = new SecureRandom();
    private final AppProperties appProperties;

    public ShortCodeGenerator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String generate() {
        int min = appProperties.shortLink().codeMinLength();
        int max = appProperties.shortLink().codeMaxLength();
        int length = min + secureRandom.nextInt(max - min + 1);
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
