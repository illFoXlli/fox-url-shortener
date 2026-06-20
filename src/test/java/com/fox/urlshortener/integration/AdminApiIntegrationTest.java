package com.fox.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminApiIntegrationTest extends IntegrationTestBase {

    @Test
    void adminCanReadUsersAndDisableAnyLink() throws Exception {
        Cookie[] userCookies = registerAndLogin("fox_admin_api");
        Cookie[] adminCookies = login("admin", "Password123");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .cookie(userCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"originalUrl":"https://example.com/admin","expiresInDays":30}
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = json(createdBody);

        mockMvc.perform(get("/api/v1/admin/users")
                .cookie(adminCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").isNotEmpty());

        mockMvc.perform(get("/api/v1/admin/links")
                .cookie(adminCookies)
                .param("login", "fox_admin_api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userLogin").value("fox_admin_api"));

        mockMvc.perform(patch("/api/v1/admin/links/{id}/status", created.get("id").asLong())
                .cookie(adminCookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"active":false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/admin/links/{id}/hard", created.get("id").asLong())
                .cookie(adminCookies))
                .andExpect(status().isNoContent());
    }
}
