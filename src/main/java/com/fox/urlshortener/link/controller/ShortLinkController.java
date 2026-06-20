package com.fox.urlshortener.link.controller;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.link.service.ShortLinkService;
import com.fox.urlshortener.link.dto.CreateShortLinkRequest;
import com.fox.urlshortener.link.dto.ShortLinkResponse;
import com.fox.urlshortener.link.dto.ShortLinkStatsResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkRequest;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import com.fox.urlshortener.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;
    private final CurrentUser currentUser;

    public ShortLinkController(ShortLinkService shortLinkService, CurrentUser currentUser) {
        this.shortLinkService = shortLinkService;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ShortLinkResponse create(
            @Valid @RequestBody CreateShortLinkRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return shortLinkService.create(request, user(authentication), servletRequest);
    }

    @GetMapping
    List<ShortLinkResponse> mine(Authentication authentication, HttpServletRequest servletRequest) {
        return shortLinkService.mine(user(authentication), servletRequest);
    }

    @GetMapping("/active")
    List<ShortLinkResponse> activeMine(Authentication authentication,
            HttpServletRequest servletRequest) {
        return shortLinkService.activeMine(user(authentication), servletRequest);
    }

    @GetMapping("/{id}")
    ShortLinkResponse get(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return shortLinkService.get(id, user(authentication), servletRequest);
    }

    @GetMapping("/{id}/stats")
    ShortLinkStatsResponse stats(@PathVariable Long id, Authentication authentication) {
        return shortLinkService.stats(id, user(authentication));
    }

    @PatchMapping("/{id}")
    ShortLinkResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShortLinkRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return shortLinkService.update(id, request, user(authentication), servletRequest);
    }

    @PatchMapping("/{id}/status")
    ShortLinkResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateShortLinkStatusRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        return shortLinkService.updateStatus(id, request, user(authentication), servletRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void softDelete(@PathVariable Long id, Authentication authentication) {
        shortLinkService.softDelete(id, user(authentication));
    }

    private User user(Authentication authentication) {
        return currentUser.require(authentication);
    }
}
