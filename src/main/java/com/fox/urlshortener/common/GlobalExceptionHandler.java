package com.fox.urlshortener.common;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String API_PREFIX = "/api/";
    private static final String API_DOCS_PREFIX = "/v3/api-docs";
    private static final String NOT_FOUND_PAGE = "static/404.html";
    private static final String SERVER_ERROR_PAGE = "static/500.html";

    private final Clock clock;
    private final ClassPathResource notFoundPage = new ClassPathResource(NOT_FOUND_PAGE);
    private final ClassPathResource serverErrorPage = new ClassPathResource(SERVER_ERROR_PAGE);

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Object> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .toList();

        return error(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Object> responseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();

        if (isPublicPageRequest(request)) {
            return htmlError(status,
                    status == HttpStatus.NOT_FOUND ? notFoundPage : serverErrorPage);
        }

        return error(status, message, request, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Object> noResource(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        if (isPublicPageRequest(request)) {
            return htmlError(HttpStatus.NOT_FOUND, notFoundPage);
        }

        return error(HttpStatus.NOT_FOUND, "Not found", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> accessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "Access denied", request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> authentication(
            AuthenticationException ex,
            HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid credentials", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> general(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unexpected error on path {}", request.getRequestURI(), ex);

        if (isPublicPageRequest(request)) {
            return htmlError(HttpStatus.INTERNAL_SERVER_ERROR, serverErrorPage);
        }

        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", request, List.of());
    }

    private ResponseEntity<Object> error(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<String> details) {
        ApiError body = new ApiError(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details);

        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<Object> htmlError(HttpStatus status, ClassPathResource page) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .body(html(page));
    }

    private String html(ClassPathResource page) {
        try {
            return StreamUtils.copyToString(page.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            log.error("Could not read error page {}", page.getPath(), ex);
            return "<!doctype html><title>Error</title><h1>Error</h1>";
        }
    }

    private boolean isPublicPageRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith(API_PREFIX) && !path.startsWith(API_DOCS_PREFIX);
    }
}
