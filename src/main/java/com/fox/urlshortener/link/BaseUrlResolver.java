package com.fox.urlshortener.link;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.config.AppProperties;

import org.springframework.stereotype.Component;

@Component
public class BaseUrlResolver {

    private final AppProperties appProperties;

    public BaseUrlResolver(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public String resolve(HttpServletRequest request) {
        return trimSlash(appProperties.shortUrlBaseUrl());
    }

    private String trimSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
