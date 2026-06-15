package com.fox.urlshortener.admin;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.auth.User;
import com.fox.urlshortener.auth.UserRepository;
import com.fox.urlshortener.link.BaseUrlResolver;
import com.fox.urlshortener.link.ShortLink;
import com.fox.urlshortener.link.ShortLinkRepository;
import com.fox.urlshortener.link.UpdateShortLinkStatusRequest;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final ShortLinkRepository shortLinkRepository;
    private final BaseUrlResolver baseUrlResolver;
    private final Clock clock;

    public AdminServiceImpl(
            UserRepository userRepository,
            ShortLinkRepository shortLinkRepository,
            BaseUrlResolver baseUrlResolver,
            Clock clock) {
        this.userRepository = userRepository;
        this.shortLinkRepository = shortLinkRepository;
        this.baseUrlResolver = baseUrlResolver;
        this.clock = clock;
    }

    @Override
    public List<AdminUserResponse> users() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
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
                user.getUsername(),
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
    public List<AdminLinkResponse> links(
            Boolean active,
            Boolean expired,
            String username,
            HttpServletRequest request) {
        return responses(shortLinkRepository.search(active, expired, username, Instant.now(clock)),
                request);
    }

    @Override
    public List<AdminLinkResponse> userLinks(Long userId, HttpServletRequest request) {
        findUser(userId);
        return responses(shortLinkRepository.findAllByUserIdOrderByCreatedAtDesc(userId), request);
    }

    @Override
    public List<AdminLinkResponse> activeUserLinks(Long userId, HttpServletRequest request) {
        findUser(userId);
        return responses(
                shortLinkRepository
                        .findAllByUserIdAndActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                                userId,
                                Instant.now(clock)),
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
        link.setActive(requestBody.active());
        return toLinkResponse(link, request);
    }

    @Override
    @Transactional
    public void softDelete(Long linkId) {
        findLink(linkId).setActive(false);
    }

    @Override
    @Transactional
    public void hardDelete(Long linkId) {
        shortLinkRepository.delete(findLink(linkId));
    }

    private AdminUserResponse toUserResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                shortLinkRepository.countByUser(user),
                shortLinkRepository.countByUserAndActiveTrueAndExpiresAtAfter(user,
                        Instant.now(clock)));
    }

    private List<AdminLinkResponse> responses(List<ShortLink> links, HttpServletRequest request) {
        return links.stream().map(link -> toLinkResponse(link, request)).toList();
    }

    private AdminLinkResponse toLinkResponse(ShortLink link, HttpServletRequest request) {
        return new AdminLinkResponse(
                link.getId(),
                link.getCode(),
                baseUrlResolver.resolve(request) + "/" + link.getCode(),
                link.getOriginalUrl(),
                link.isActive(),
                link.getClickCount(),
                link.getCreatedAt(),
                link.getUpdatedAt(),
                link.getExpiresAt(),
                link.getUser().getId(),
                link.getUser().getUsername());
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
}
