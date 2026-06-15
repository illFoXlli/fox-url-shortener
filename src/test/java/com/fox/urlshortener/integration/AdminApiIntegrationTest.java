package com.fox.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminApiIntegrationTest extends IntegrationTestBase {

    @Test
    void adminCanReadUsersAndDisableAnyLink() throws Exception {
        String userToken = registerAndLogin("fox_admin_api");
        String adminToken = login("admin", "Password123");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .header("Authorization", "Bearer " + userToken)
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
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").isNotEmpty());

        mockMvc.perform(get("/api/v1/admin/links")
                .header("Authorization", "Bearer " + adminToken)
                .param("username", "fox_admin_api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("fox_admin_api"));

        mockMvc.perform(patch("/api/v1/admin/links/{id}/status", created.get("id").asLong())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"active":false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }
}
