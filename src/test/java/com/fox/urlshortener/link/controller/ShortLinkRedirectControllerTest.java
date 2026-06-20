package com.fox.urlshortener.link.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fox.urlshortener.link.service.ShortLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ShortLinkRedirectControllerTest {

    @Mock
    private ShortLinkService shortLinkService;

    @Test
    void homeReturnsServiceStatus() {
        var body = controller().home();

        assertThat(body).containsEntry("service", "Fox URL Shortener")
                .containsEntry("status", "OK");
    }

    @Test
    void redirectReturnsFoundLocationForKnownCode() throws Exception {
        when(shortLinkService.redirect("aB12xZ")).thenReturn("https://example.com");

        var response = controller().redirect("aB12xZ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("https://example.com");
    }

    @Test
    void redirectReturnsGoneForExpiredCode() throws Exception {
        when(shortLinkService.redirect("aB12xZ"))
                .thenThrow(new ResponseStatusException(HttpStatus.GONE));

        var response = controller().redirect("aB12xZ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
    }

    @Test
    void redirectReturnsStaticNotFoundPageForMissingCode() throws Exception {
        when(shortLinkService.redirect("aB12xZ"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        var response = controller().redirect("aB12xZ");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        assertThat(response.getBody()).contains("Link unavailable");
    }

    @Test
    void redirectRethrowsUnexpectedResponseStatus() {
        when(shortLinkService.redirect("aB12xZ"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> controller().redirect("aB12xZ"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502");
    }

    private ShortLinkRedirectController controller() {
        return new ShortLinkRedirectController(shortLinkService);
    }
}
