package com.fox.urlshortener.integration;

import java.util.List;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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
        registry.add("spring.application.name", () -> "fox-url-shortener-test");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jackson.default-property-inclusion", () -> "non_null");
        registry.add("server.forward-headers-strategy", () -> "framework");
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("springdoc.api-docs.path", () -> "/v3/api-docs");
        registry.add("springdoc.swagger-ui.path", () -> "/swagger-ui/index.html");
        registry.add("app.admin.login", () -> "admin");
        registry.add("app.admin.password", () -> "Password123");
        registry.add("app.admin.display-name", () -> "Test Admin");
        registry.add("app.base-url", () -> "http://localhost:3396");
        registry.add("app.short-url-base-url", () -> "http://localhost:3396");
        registry.add("app.jwt.secret", () -> "test_jwt_secret_with_more_than_32_chars");
        registry.add("app.jwt.access-expiration-minutes", () -> "15");
        registry.add("app.jwt.refresh-expiration-days", () -> "30");
        registry.add("app.cookie.access-token-name", () -> "fox_access_token");
        registry.add("app.cookie.refresh-token-name", () -> "fox_refresh_token");
        registry.add("app.cookie.secure", () -> "false");
        registry.add("app.cookie.same-site", () -> "Lax");
        registry.add("app.cookie.domain", () -> "");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:3395");
        registry.add("app.forwarded.proto-header", () -> "X-Forwarded-Proto");
        registry.add("app.forwarded.host-header", () -> "X-Forwarded-Host");
        registry.add("app.forwarded.port-header", () -> "X-Forwarded-Port");
        registry.add("app.short-link.code-min-length", () -> "6");
        registry.add("app.short-link.code-max-length", () -> "8");
        registry.add("app.short-link.default-expiration-days", () -> "30");
        registry.add(
                "logging.level.org.springframework.security.config.annotation.authentication.configuration.InitializeUserDetailsBeanManagerConfigurer",
                () -> "error");
        registry.add("logging.level.org.springdoc.core.events.SpringDocAppInitializer",
                () -> "error");
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
