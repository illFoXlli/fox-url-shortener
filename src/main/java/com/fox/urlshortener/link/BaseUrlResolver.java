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
        String host = header(request, appProperties.forwarded().hostHeader());
        String port = header(request, appProperties.forwarded().portHeader());

        String resolvedProto = firstText(proto, request.getScheme(), shortUrlUri.getScheme());
        String resolvedHost = firstText(host, request.getServerName(), shortUrlUri.getHost());
        String resolvedPort = resolvePort(port, resolvedProto, resolvedHost, request, shortUrlUri);

        return resolvedProto + "://" + resolvedHost + resolvedPort;
    }

    private String resolvePort(String requestPort, String proto, String host,
            HttpServletRequest request, URI shortUrlUri) {
        if (hasText(requestPort) && !host.contains(":")) {
            return ":" + requestPort;
        }

        if (!host.contains(":") && request.getServerPort() > 0) {
            int requestPortValue = request.getServerPort();
            if (!defaultPort(proto, requestPortValue)) {
                return ":" + requestPortValue;
            }
            return "";
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

    private String firstText(String first, String second, String fallback) {
        if (hasText(first)) {
            return first;
        }
        if (hasText(second)) {
            return second;
        }
        return fallback;
    }

    private boolean defaultPort(String proto, int port) {
        return ("http".equalsIgnoreCase(proto) && port == 80)
                || ("https".equalsIgnoreCase(proto) && port == 443);
    }
}
