package com.fox.urlshortener.link;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.auth.User;

public interface ShortLinkService {

    ShortLinkResponse create(CreateShortLinkRequest request, User user,
            HttpServletRequest servletRequest);

    List<ShortLinkResponse> mine(User user, HttpServletRequest servletRequest);

    List<ShortLinkResponse> activeMine(User user, HttpServletRequest servletRequest);

    ShortLinkResponse get(Long id, User user, HttpServletRequest servletRequest);

    ShortLinkResponse update(Long id, UpdateShortLinkRequest request, User user,
            HttpServletRequest servletRequest);

    ShortLinkResponse updateStatus(Long id, UpdateShortLinkStatusRequest request, User user,
            HttpServletRequest servletRequest);

    void softDelete(Long id, User user);

    void hardDelete(Long id, User user);

    ShortLink redirect(String code);
}
