package com.fox.urlshortener.link.service;

import com.fox.urlshortener.auth.model.User;
import com.fox.urlshortener.link.dto.CreateShortLinkRequest;
import com.fox.urlshortener.link.dto.ShortLinkResponse;
import com.fox.urlshortener.link.dto.ShortLinkStatsResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkRequest;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface ShortLinkService {

    ShortLinkResponse create(CreateShortLinkRequest request, User user,
            HttpServletRequest servletRequest);

    List<ShortLinkResponse> mine(User user, HttpServletRequest servletRequest);

    List<ShortLinkResponse> activeMine(User user, HttpServletRequest servletRequest);

    ShortLinkResponse get(Long id, User user, HttpServletRequest servletRequest);

    ShortLinkStatsResponse stats(Long id, User user);

    ShortLinkResponse update(Long id, UpdateShortLinkRequest request, User user,
            HttpServletRequest servletRequest);

    ShortLinkResponse updateStatus(Long id, UpdateShortLinkStatusRequest request, User user,
            HttpServletRequest servletRequest);

    void softDelete(Long id, User user);

    String redirect(String code);
}
