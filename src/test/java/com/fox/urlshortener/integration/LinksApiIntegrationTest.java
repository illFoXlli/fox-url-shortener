package com.fox.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class LinksApiIntegrationTest extends IntegrationTestBase {

    @Test
    void userCanManageOwnLinks() throws Exception {
        String token = registerAndLogin("fox_links");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .header("Authorization", "Bearer " + token)
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Host", "api.fox.kh.ua")
                .header("X-Forwarded-Port", "8443")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"originalUrl":"https://example.com/path","expiresInDays":30}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortUrl").value(org.hamcrest.Matchers.startsWith(
                        "https://api.fox.kh.ua:8443/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode created = json(createdBody);
        long id = created.get("id").asLong();

        mockMvc.perform(get("/api/v1/links")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/v1/links/active")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));

        mockMvc.perform(patch("/api/v1/links/{id}/status", id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"active":false}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/v1/links/{id}/hard", id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }
}
