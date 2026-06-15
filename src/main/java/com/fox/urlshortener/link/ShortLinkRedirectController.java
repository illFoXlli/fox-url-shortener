package com.fox.urlshortener.link;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ShortLinkRedirectController {

    private final ShortLinkService shortLinkService;

    public ShortLinkRedirectController(ShortLinkService shortLinkService) {
        this.shortLinkService = shortLinkService;
    }

    @GetMapping("/")
    Map<String, String> home() {
        return Map.of("service", "Fox URL Shortener", "status", "OK");
    }

    @GetMapping("/{code:[a-zA-Z0-9]{6,8}}")
    ResponseEntity<Void> redirect(@PathVariable String code) {
        ShortLink link = shortLinkService.redirect(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.getOriginalUrl()))
                .build();
    }
}
