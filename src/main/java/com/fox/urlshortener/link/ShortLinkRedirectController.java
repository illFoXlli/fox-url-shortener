package com.fox.urlshortener.link;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;

@RestController
public class ShortLinkRedirectController {

    private static final String NOT_FOUND_PAGE = "static/404.html";

    private final ShortLinkService shortLinkService;
    private final ClassPathResource notFoundPage = new ClassPathResource(NOT_FOUND_PAGE);

    public ShortLinkRedirectController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping("/")
    Map<String, String> home() {
        return Map.of("service", "Fox URL Shortener", "status", "OK");
    }

    @GetMapping("/{code:[a-zA-Z0-9]{6,8}}")
    ResponseEntity<?> redirect(@PathVariable String code) throws IOException {
        try {
            ShortLink link = shortLinkService.redirect(code);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(link.getOriginalUrl()))
                    .build();
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() != HttpStatus.NOT_FOUND.value()) {
                throw ex;
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(notFoundHtml());
        }
    }

    private String notFoundHtml() throws IOException {
        return StreamUtils.copyToString(notFoundPage.getInputStream(), StandardCharsets.UTF_8);
    }
}
