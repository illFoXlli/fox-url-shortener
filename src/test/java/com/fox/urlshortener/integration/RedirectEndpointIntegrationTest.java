package com.fox.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class RedirectEndpointIntegrationTest extends IntegrationTestBase {

    @Test
    void publicRedirectSendsUserToOriginalUrl() throws Exception {
        String token = registerAndLogin("fox_redirect");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"originalUrl":"https://example.com/redirect","expiresInDays":30}
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = json(createdBody);

        mockMvc.perform(get("/{code}", created.get("code").asText()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/redirect"));
    }
}
