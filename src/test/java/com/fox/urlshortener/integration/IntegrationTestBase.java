package com.fox.urlshortener.integration;

import java.util.List;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
abstract class IntegrationTestBase {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.admin.login", () -> "admin");
        registry.add("app.admin.password", () -> "Password123");
        registry.add("app.jwt.secret", () -> "change_me_to_long_secret_change_me_to_long_secret");
        registry.add("app.base-url", () -> "http://localhost:3396");
    }

    @BeforeEach
    void ensureAdminLoginWorks() throws Exception {
        login("admin", "Password123");
    }

    Cookie[] registerAndLogin(String login) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"%s","password":"Password123"}
                        """.formatted(login)))
                .andExpect(status().isCreated());
        return login(login, "Password123");
    }

    Cookie[] login(String login, String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"%s","password":"%s"}
                        """.formatted(login, password)))
                .andExpect(status().isOk())
                .andReturn();
        return cookies(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE));
    }

    JsonNode json(String body) throws Exception {
        return objectMapper.readTree(body);
    }

    Cookie[] cookies(List<String> setCookieHeaders) {
        return setCookieHeaders.stream()
                .map(this::cookie)
                .toArray(Cookie[]::new);
    }

    private Cookie cookie(String setCookieHeader) {
        String[] parts = setCookieHeader.split(";", 2)[0].split("=", 2);
        return new Cookie(parts[0], parts.length == 2 ? parts[1] : "");
    }
}
