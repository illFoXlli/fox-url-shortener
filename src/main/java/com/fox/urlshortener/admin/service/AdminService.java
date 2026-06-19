package com.fox.urlshortener.admin.service;

import com.fox.urlshortener.admin.dto.AdminLinkResponse;
import com.fox.urlshortener.admin.dto.AdminUserDetailsResponse;
import com.fox.urlshortener.admin.dto.AdminUserResponse;
import com.fox.urlshortener.link.dto.UpdateShortLinkStatusRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

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
