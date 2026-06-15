package com.fox.urlshortener.admin;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import com.fox.urlshortener.link.UpdateShortLinkStatusRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    List<AdminUserResponse> users() {
        return adminService.users();
    }

    @GetMapping("/users/{userId}")
    AdminUserDetailsResponse user(@PathVariable Long userId) {
        return adminService.user(userId);
    }

    @GetMapping("/links")
    List<AdminLinkResponse> links(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean expired,
            @RequestParam(required = false) String login,
            HttpServletRequest request) {
        return adminService.links(active, expired, login, request);
    }

    @GetMapping("/users/{userId}/links")
    List<AdminLinkResponse> userLinks(@PathVariable Long userId, HttpServletRequest request) {
        return adminService.userLinks(userId, request);
    }

    @GetMapping("/users/{userId}/links/active")
    List<AdminLinkResponse> activeUserLinks(@PathVariable Long userId, HttpServletRequest request) {
        return adminService.activeUserLinks(userId, request);
    }

    @GetMapping("/links/{linkId}")
    AdminLinkResponse link(@PathVariable Long linkId, HttpServletRequest request) {
        return adminService.link(linkId, request);
    }

    @PatchMapping("/links/{linkId}/status")
    AdminLinkResponse updateStatus(
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateShortLinkStatusRequest requestBody,
            HttpServletRequest request) {
        return adminService.updateStatus(linkId, requestBody, request);
    }

    @DeleteMapping("/links/{linkId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void softDelete(@PathVariable Long linkId) {
        adminService.softDelete(linkId);
    }

    @DeleteMapping("/links/{linkId}/hard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void hardDelete(@PathVariable Long linkId) {
        adminService.hardDelete(linkId);
    }
}
