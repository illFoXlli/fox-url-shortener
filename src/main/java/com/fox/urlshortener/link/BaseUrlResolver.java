package com.fox.urlshortener.link;

import java.net.URI;

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
        URI shortUrlUri = URI.create(trimSlash(appProperties.shortUrlBaseUrl()));

        String proto = header(request, appProperties.forwarded().protoHeader());
        String port = header(request, appProperties.forwarded().portHeader());

        String resolvedProto = hasText(proto) ? proto : shortUrlUri.getScheme();
        String resolvedHost = shortUrlUri.getHost();
        String resolvedPort = resolvePort(port, shortUrlUri);

        return resolvedProto + "://" + resolvedHost + resolvedPort;
    }

    private String resolvePort(String requestPort, URI shortUrlUri) {
        if (hasText(requestPort)) {
            return ":" + requestPort;
        }

        int configPort = shortUrlUri.getPort();
        if (configPort == -1) {
            return "";
        }

        return ":" + configPort;
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.split(",")[0].trim();
    }

    private String trimSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

// package com.fox.urlshortener.link;
//
// import jakarta.servlet.http.HttpServletRequest;
//
// import com.fox.urlshortener.config.AppProperties;
//
// import org.springframework.stereotype.Component;
//
// @Component
// public class BaseUrlResolver {
//
// private final AppProperties appProperties;
//
// public BaseUrlResolver(AppProperties appProperties) {
// this.appProperties = appProperties;
// }
//
// public String resolve(HttpServletRequest request) {
// String proto = header(request, appProperties.forwarded().protoHeader());
// String host = header(request, appProperties.forwarded().hostHeader());
// String port = header(request, appProperties.forwarded().portHeader());
// if (proto == null || host == null) {
// return trimSlash(appProperties.baseUrl());
// }
// String normalizedPort = port == null || host.contains(":") ? "" : ":" + port;
// return proto + "://" + host + normalizedPort;
// }
//
// private String header(HttpServletRequest request, String name) {
// String value = request.getHeader(name);
// if (value == null || value.isBlank()) {
// return null;
// }
// return value.split(",")[0].trim();
// }
//
// private String trimSlash(String value) {
// if (value.endsWith("/")) {
// return value.substring(0, value.length() - 1);
// }
// return value;
// }
// }
