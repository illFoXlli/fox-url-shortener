package com.fox.urlshortener.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(
            Clock.fixed(Instant.parse("2026-06-20T10:15:30Z"), ZoneOffset.UTC));

    @Test
    void validationReturnsApiErrorWithFieldDetails() throws Exception {
        MethodArgumentNotValidException exception = validationException();

        var response = handler.validation(exception, request("/api/v1/auth/register"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOfSatisfying(ApiError.class, error -> {
            assertThat(error.timestamp()).isEqualTo(Instant.parse("2026-06-20T10:15:30Z"));
            assertThat(error.status()).isEqualTo(400);
            assertThat(error.error()).isEqualTo("Bad Request");
            assertThat(error.message()).isEqualTo("Validation failed");
            assertThat(error.path()).isEqualTo("/api/v1/auth/register");
            assertThat(error.details()).containsExactly("login must not be blank");
        });
    }

    @Test
    void responseStatusReturnsApiErrorForApiRequest() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.GONE,
                "Link expired");

        var response = handler.responseStatus(exception, request("/api/v1/links/aB12xZ"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GONE);
        assertThat(response.getBody()).isInstanceOfSatisfying(ApiError.class, error -> {
            assertThat(error.message()).isEqualTo("Link expired");
            assertThat(error.details()).isEmpty();
        });
    }

    @Test
    void responseStatusReturnsStaticPageForPublicNotFound() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND);

        var response = handler.responseStatus(exception, request("/missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        assertThat(response.getBody()).asString().contains("Link unavailable");
    }

    @Test
    void accessDeniedReturnsForbiddenApiError() {
        var response = handler.accessDenied(new AccessDeniedException("denied"),
                request("/api/v1/admin/users"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isInstanceOfSatisfying(ApiError.class,
                error -> assertThat(error.message()).isEqualTo("Access denied"));
    }

    @Test
    void authenticationReturnsUnauthorizedApiError() {
        var response = handler.authentication(new BadCredentialsException("bad"),
                request("/api/v1/auth/login"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isInstanceOfSatisfying(ApiError.class,
                error -> assertThat(error.message()).isEqualTo("Invalid credentials"));
    }

    @Test
    void generalReturnsStaticPageForPublicRequest() {
        var response = handler.general(new IllegalStateException("boom"), request("/"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.TEXT_HTML);
        assertThat(response.getBody()).asString().contains("Something went wrong");
    }

    @Test
    void clockConfigCreatesUtcClock() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    private MethodArgumentNotValidException validationException() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(
                new Object(), "request");
        bindingResult.addError(new FieldError("request", "login", "must not be blank"));
        return new MethodArgumentNotValidException(methodParameter(), bindingResult);
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        Method method = getClass().getDeclaredMethod("dummy", RegisterCommand.class);
        return new MethodParameter(method, 0);
    }

    private MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    @SuppressWarnings("unused")
    private void dummy(RegisterCommand command) {
        // Intentionally empty: this method exists only to create MethodParameter
        // metadata for tests.
    }

    private record RegisterCommand(String login) {
    }
}
