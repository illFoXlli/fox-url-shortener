package com.fox.urlshortener.link.service;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.auth.model.UserRole;
import com.fox.urlshortener.config.AppProperties;
import com.fox.urlshortener.link.dto.CreateShortLinkRequest;
import com.fox.urlshortener.link.dto.ShortLinkResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkRequest;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import com.fox.urlshortener.link.model.ShortLink;
import com.fox.urlshortener.link.repository.ShortLinkRepository;
import com.fox.urlshortener.link.support.BaseUrlResolver;
import com.fox.urlshortener.link.support.ShortCodeGenerator;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShortLinkServiceImpl implements ShortLinkService {

    private static final int MAX_CODE_ATTEMPTS = 20;

    private final ShortLinkRepository shortLinkRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final BaseUrlResolver baseUrlResolver;
    private final ShortLinkRedirectCache redirectCache;
    private final AppProperties appProperties;
    private final Clock clock;

    public ShortLinkServiceImpl(
            ShortLinkRepository shortLinkRepository,
            ShortCodeGenerator shortCodeGenerator,
            BaseUrlResolver baseUrlResolver,
            ShortLinkRedirectCache redirectCache,
            AppProperties appProperties,
            Clock clock) {
        this.shortLinkRepository = shortLinkRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.baseUrlResolver = baseUrlResolver;
        this.redirectCache = redirectCache;
        this.appProperties = appProperties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ShortLinkResponse create(CreateShortLinkRequest request, User user,
            HttpServletRequest servletRequest) {
        int days = request.expiresInDays() == null
                ? appProperties.shortLink().defaultExpirationDays()
                : request.expiresInDays();
        ShortLink link = shortLinkRepository.save(new ShortLink(
                uniqueCode(),
                request.originalUrl(),
                Instant.now(clock).plusSeconds(days * 86_400L),
                user));
        return toResponse(link, servletRequest);
    }

    @Override
    public List<ShortLinkResponse> mine(User user, HttpServletRequest servletRequest) {
        return responses(shortLinkRepository.findAllByUserOrderByCreatedAtDesc(user),
                servletRequest);
    }

    @Override
    public List<ShortLinkResponse> activeMine(User user, HttpServletRequest servletRequest) {
        return responses(
                shortLinkRepository.findAllByUserAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                        user,
                        Instant.now(clock)),
                servletRequest);
    }

    @Override
    public ShortLinkResponse get(Long id, User user, HttpServletRequest servletRequest) {
        return toResponse(ownedOrAdmin(id, user), servletRequest);
    }

    @Override
    @Transactional
    public ShortLinkResponse update(
            Long id,
            UpdateShortLinkRequest request,
            User user,
            HttpServletRequest servletRequest) {
        ShortLink link = ownedOrAdmin(id, user);
        flushAndEvictRedirectCache(link.getCode());
        Instant expiresAt = request.expiresInDays() == null
                ? null
                : Instant.now(clock).plusSeconds(request.expiresInDays() * 86_400L);
        link.update(request.originalUrl(), expiresAt, request.active());
        return toResponse(link, servletRequest);
    }

    @Override
    @Transactional
    public ShortLinkResponse updateStatus(
            Long id,
            UpdateShortLinkStatusRequest request,
            User user,
            HttpServletRequest servletRequest) {
        ShortLink link = ownedOrAdmin(id, user);
        flushAndEvictRedirectCache(link.getCode());
        link.setActive(request.active());
        return toResponse(link, servletRequest);
    }

    @Override
    @Transactional
    public void softDelete(Long id, User user) {
        ShortLink link = ownedOrAdmin(id, user);
        flushAndEvictRedirectCache(link.getCode());
        link.setActive(false);
    }

    @Override
    @Transactional
    public void hardDelete(Long id, User user) {
        ShortLink link = ownedOrAdmin(id, user);
        flushAndEvictRedirectCache(link.getCode());
        shortLinkRepository.delete(link);
    }

    @Override
    @Transactional
    public String redirect(String code) {
        Instant now = Instant.now(clock);

        return redirectCache.findOriginalUrl(code)
                .filter(originalUrl -> redirectCache.incrementClickCount(code))
                .orElseGet(() -> redirectFromDatabase(code, now));
    }

    private String redirectFromDatabase(String code, Instant now) {
        ShortLink link = shortLinkRepository.findActiveByCode(code, now)
                .orElseGet(() -> redirectFallback(code, now));

        redirectCache.putOriginalUrl(code, link.getOriginalUrl(),
                java.time.Duration.between(now, link.getExpiresAt()));
        if (!redirectCache.incrementClickCount(code)) {
            shortLinkRepository.incrementClickCount(code, now);
        }
        return link.getOriginalUrl();
    }

    private ShortLink redirectFallback(String code, Instant now) {
        ShortLink link = shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Short link not found"));
        if (!link.isActive()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found");
        }
        if (!link.getExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Short link expired");
        }
        return link;
    }

    private ShortLink ownedOrAdmin(Long id, User user) {
        ShortLink link = shortLinkRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Short link not found"));
        if (user.getRole() == UserRole.ADMIN || link.getUser().getId().equals(user.getId())) {
            return link;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found");
    }

    private String uniqueCode() {
        return Stream.generate(shortCodeGenerator::generate)
                .limit(MAX_CODE_ATTEMPTS)
                .filter(code -> !shortLinkRepository.existsByCode(code))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Could not generate short code"));
    }

    private void flushAndEvictRedirectCache(String code) {
        long clicks = redirectCache.drainClickCount(code);
        if (clicks > 0) {
            shortLinkRepository.addClickCount(code, clicks, Instant.now(clock));
        }
        redirectCache.evict(code);
    }

    private List<ShortLinkResponse> responses(List<ShortLink> links, HttpServletRequest request) {
        return links.stream().map(link -> toResponse(link, request)).toList();
    }

    ShortLinkResponse toResponse(ShortLink link, HttpServletRequest request) {
        return new ShortLinkResponse(
                link.getId(),
                link.getCode(),
                baseUrlResolver.resolve(request) + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.isActive(),
                link.getClickCount(),
                link.getCreatedAt(),
                link.getUpdatedAt(),
                link.getExpiresAt());
    }
}
