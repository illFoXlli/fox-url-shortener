package com.fox.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;

import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiIntegrationTest extends IntegrationTestBase {

    @Test
    void registerLoginRefreshAndMeUseHttpOnlyCookies() throws Exception {
        String login = "fox_auth";
        var registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"%s","password":"Password123"}
                        """.formatted(login)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.login").value(login))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        Cookie[] registerCookies = cookies(
                registerResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE));

        mockMvc.perform(get("/api/v1/auth/me")
                .cookie(registerCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value(login))
                .andExpect(jsonPath("$.role").value("USER"));

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"%s","password":"Password123"}
                        """.formatted(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.login").value(login))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andReturn();

        Cookie[] loginCookies = cookies(
                loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .cookie(loginCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.login").value(login))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }
}
