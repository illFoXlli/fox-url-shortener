package com.fox.urlshortener.link.service;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.link.dto.CreateShortLinkRequest;
import com.fox.urlshortener.link.dto.ShortLinkResponse;
import com.fox.urlshortener.link.dto.ShortLinkStatsResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkRequest;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShortLinkService {

    ShortLinkResponse create(CreateShortLinkRequest request, User user,
            HttpServletRequest servletRequest);

    Page<ShortLinkResponse> mine(User user, HttpServletRequest servletRequest, Pageable pageable);

    Page<ShortLinkResponse> activeMine(User user, HttpServletRequest servletRequest,
            Pageable pageable);

    ShortLinkResponse get(Long id, User user, HttpServletRequest servletRequest);

    ShortLinkStatsResponse stats(Long id, User user);

    ShortLinkResponse update(Long id, UpdateShortLinkRequest request, User user,
            HttpServletRequest servletRequest);

    ShortLinkResponse updateStatus(Long id, UpdateShortLinkStatusRequest request, User user,
            HttpServletRequest servletRequest);

    void softDelete(Long id, User user);

    String redirect(String code);
}
