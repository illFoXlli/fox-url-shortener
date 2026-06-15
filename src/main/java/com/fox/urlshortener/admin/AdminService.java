package com.fox.urlshortener.admin;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import com.fox.urlshortener.link.UpdateShortLinkStatusRequest;

public interface AdminService {

    List<AdminUserResponse> users();

    AdminUserDetailsResponse user(Long userId);

    List<AdminLinkResponse> links(
            Boolean active,
            Boolean expired,
            String login,
            HttpServletRequest request);

    List<AdminLinkResponse> userLinks(Long userId, HttpServletRequest request);

    List<AdminLinkResponse> activeUserLinks(Long userId, HttpServletRequest request);

    AdminLinkResponse link(Long linkId, HttpServletRequest request);

    AdminLinkResponse updateStatus(
            Long linkId,
            UpdateShortLinkStatusRequest requestBody,
            HttpServletRequest request);

    void softDelete(Long linkId);

    void hardDelete(Long linkId);
}
