package com.fox.urlshortener.admin.service;

import com.fox.urlshortener.admin.dto.AdminLinkResponse;
import com.fox.urlshortener.admin.dto.AdminUserDetailsResponse;
import com.fox.urlshortener.admin.dto.AdminUserResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminService {

    Page<AdminUserResponse> users(Pageable pageable);

    AdminUserDetailsResponse user(Long userId);

    Page<AdminLinkResponse> links(
            Boolean active,
            Boolean expired,
            String login,
            HttpServletRequest request,
            Pageable pageable);

    Page<AdminLinkResponse> userLinks(Long userId, HttpServletRequest request, Pageable pageable);

    Page<AdminLinkResponse> activeUserLinks(Long userId, HttpServletRequest request,
            Pageable pageable);

    AdminLinkResponse link(Long linkId, HttpServletRequest request);

    AdminLinkResponse updateStatus(
            Long linkId,
            UpdateShortLinkStatusRequest requestBody,
            HttpServletRequest request);

    void softDelete(Long linkId);

    void hardDelete(Long linkId);
}
