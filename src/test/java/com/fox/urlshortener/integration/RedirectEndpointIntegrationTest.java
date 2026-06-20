package com.fox.urlshortener.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fox.urlshortener.link.repository.ShortLinkRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class RedirectEndpointIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ShortLinkRepository shortLinkRepository;

    @Test
    void publicRedirectSendsUserToOriginalUrl() throws Exception {
        Cookie[] cookies = registerAndLogin("fox_redirect");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .cookie(cookies)
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

    @Test
    void disabledLinkReturnsStaticNotFoundPage() throws Exception {
        Cookie[] cookies = registerAndLogin("fox_redirect_disabled");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .cookie(cookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"originalUrl":"https://example.com/disabled","expiresInDays":30}
                        """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = json(createdBody);

        mockMvc.perform(patch("/api/v1/links/{id}/status", created.get("id").asLong())
                .cookie(cookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"active":false}
                        """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/{code}", created.get("code").asText()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Link unavailable")));
    }

    @Test
    void expiredLinkReturnsGone() throws Exception {
        Cookie[] cookies = registerAndLogin("fox_redirect_expired");

        String createdBody = mockMvc.perform(post("/api/v1/links")
                .cookie(cookies)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                                                {"originalUrl":"https://example.com/expired","expiresInDays":30}
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode created = json(createdBody);

        shortLinkRepository.findById(created.get("id").asLong()).ifPresent(link -> {
            link.update(null, Instant.parse("2026-01-01T00:00:00Z"), null);
            shortLinkRepository.saveAndFlush(link);
        });

        mockMvc.perform(get("/{code}", created.get("code").asText()))
                .andExpect(status().isGone());
    }

    @Test
    void unknownPublicPathReturnsStaticNotFoundPage() throws Exception {
        mockMvc.perform(get("/sdfsdfadfadsfsdf"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Link unavailable")));
    }
}
