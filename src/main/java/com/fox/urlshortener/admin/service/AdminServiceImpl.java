package com.fox.urlshortener.admin.service;

import com.fox.urlshortener.admin.dto.AdminLinkResponse;
import com.fox.urlshortener.admin.dto.AdminUserDetailsResponse;
import com.fox.urlshortener.admin.dto.AdminUserResponse;
import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.auth.repository.UserRepository;
import com.fox.urlshortener.link.service.ShortLinkRedirectCache;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import com.fox.urlshortener.link.support.BaseUrlResolver;
import com.fox.urlshortener.link.model.ShortLink;
import com.fox.urlshortener.link.repository.ShortLinkRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkRedirectCache redirectCache;
    private final BaseUrlResolver baseUrlResolver;
    private final Clock clock;

    public AdminServiceImpl(
            UserRepository userRepository,
            ShortLinkRepository shortLinkRepository,
            ShortLinkRedirectCache redirectCache,
            BaseUrlResolver baseUrlResolver,
            Clock clock) {
        this.userRepository = userRepository;
        this.shortLinkRepository = shortLinkRepository;
        this.redirectCache = redirectCache;
        this.baseUrlResolver = baseUrlResolver;
        this.clock = clock;
    }

    @Override
    public Page<AdminUserResponse> users(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Override
    public AdminUserDetailsResponse user(Long userId) {
        User user = findUser(userId);
        long totalClicks = shortLinkRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .mapToLong(ShortLink::getClickCount)
                .sum();
        return new AdminUserDetailsResponse(
                user.getId(),
                user.getLogin(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                shortLinkRepository.countByUser(user),
                shortLinkRepository.countByUserAndActiveTrueAndExpiresAtAfter(user,
                        Instant.now(clock)),
                totalClicks);
    }

    @Override
    public Page<AdminLinkResponse> links(
            Boolean active,
            Boolean expired,
            String login,
            HttpServletRequest request,
            Pageable pageable) {
        return responses(shortLinkRepository.search(
                active,
                expired,
                login,
                Instant.now(clock),
                pageable), request);
    }

    @Override
    public Page<AdminLinkResponse> userLinks(
            Long userId,
            HttpServletRequest request,
            Pageable pageable) {
        findUser(userId);
        return responses(shortLinkRepository.findAllByUserId(userId, pageable), request);
    }

    @Override
    public Page<AdminLinkResponse> activeUserLinks(
            Long userId,
            HttpServletRequest request,
            Pageable pageable) {
        findUser(userId);
        return responses(
                shortLinkRepository.findAllByUserIdAndActiveTrueAndExpiresAtAfter(
                        userId,
                        Instant.now(clock),
                        pageable),
                request);
    }

    @Override
    public AdminLinkResponse link(Long linkId, HttpServletRequest request) {
        return toLinkResponse(findLink(linkId), request);
    }

    @Override
    @Transactional
    public AdminLinkResponse updateStatus(
            Long linkId,
            UpdateShortLinkStatusRequest requestBody,
            HttpServletRequest request) {
        ShortLink link = findLink(linkId);
        flushAndEvictRedirectCache(link.getCode());
        link.setActive(requestBody.active());
        return toLinkResponse(link, request);
    }

    @Override
    @Transactional
    public void softDelete(Long linkId) {
        ShortLink link = findLink(linkId);
        flushAndEvictRedirectCache(link.getCode());
        link.setActive(false);
    }

    @Override
    @Transactional
    public void hardDelete(Long linkId) {
        ShortLink link = findLink(linkId);
        flushAndEvictRedirectCache(link.getCode());
        shortLinkRepository.delete(link);
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getLogin(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                shortLinkRepository.countByUser(user),
                shortLinkRepository.countByUserAndActiveTrueAndExpiresAtAfter(user,
                        Instant.now(clock)));
    }

    private Page<AdminLinkResponse> responses(Page<ShortLink> links, HttpServletRequest request) {
        return links.map(link -> toLinkResponse(link, request));
    }

    private AdminLinkResponse toLinkResponse(ShortLink link, HttpServletRequest request) {
        return new AdminLinkResponse(
                link.getId(),
                link.getCode(),
                baseUrlResolver.resolve() + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.isActive(),
                link.getClickCount(),
                link.getCreatedAt(),
                link.getUpdatedAt(),
                link.getExpiresAt(),
                link.getUser().getId(),
                link.getUser().getLogin());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private ShortLink findLink(Long linkId) {
        return shortLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Short link not found"));
    }

    private void flushAndEvictRedirectCache(String code) {
        long clicks = redirectCache.drainClickCount(code);
        if (clicks > 0) {
            shortLinkRepository.addClickCount(code, clicks, Instant.now(clock));
        }
        redirectCache.evict(code);
    }
}
