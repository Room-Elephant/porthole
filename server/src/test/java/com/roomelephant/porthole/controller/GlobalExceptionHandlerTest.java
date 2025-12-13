package com.roomelephant.porthole.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("handleResponseStatusException")
    class HandleResponseStatusException {

        @Test
        @DisplayName("should return ProblemDetail with correct status for NOT_FOUND")
        void shouldReturnProblemDetailWithCorrectStatusForNotFound() {
            ResponseStatusException exception = new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Container not found");

            ProblemDetail result = exceptionHandler.handleResponseStatusException(exception);

            assertEquals(404, result.getStatus());
            assertEquals("Container not found", result.getDetail());
            assertEquals(URI.create("about:blank"), result.getType());
        }

        @Test
        @DisplayName("should return ProblemDetail with correct status for BAD_GATEWAY")
        void shouldReturnProblemDetailWithCorrectStatusForBadGateway() {
            ResponseStatusException exception = new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Docker is not reachable");

            ProblemDetail result = exceptionHandler.handleResponseStatusException(exception);

            assertEquals(502, result.getStatus());
            assertEquals("Docker is not reachable", result.getDetail());
        }

        @Test
        @DisplayName("should handle null reason")
        void shouldHandleNullReason() {
            ResponseStatusException exception = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

            ProblemDetail result = exceptionHandler.handleResponseStatusException(exception);

            assertEquals(500, result.getStatus());
        }
    }

    @Nested
    @DisplayName("handleIllegalArgument")
    class HandleIllegalArgument {

        @Test
        @DisplayName("should return ProblemDetail with BAD_REQUEST status")
        void shouldReturnProblemDetailWithBadRequestStatus() {
            IllegalArgumentException exception = new IllegalArgumentException("Invalid parameter");

            ProblemDetail result = exceptionHandler.handleIllegalArgument(exception);

            assertEquals(400, result.getStatus());
            assertEquals("Invalid parameter", result.getDetail());
            assertEquals("Bad Request", result.getTitle());
            assertEquals(URI.create("about:blank"), result.getType());
        }

        @Test
        @DisplayName("should handle empty message")
        void shouldHandleEmptyMessage() {
            IllegalArgumentException exception = new IllegalArgumentException("");

            ProblemDetail result = exceptionHandler.handleIllegalArgument(exception);

            assertEquals(400, result.getStatus());
            assertEquals("", result.getDetail());
        }
    }

    @Nested
    @DisplayName("handleNoResourceFound")
    class HandleNoResourceFound {

        @Test
        @DisplayName("should return ProblemDetail with NOT_FOUND status")
        void shouldReturnProblemDetailWithNotFoundStatus() {
            org.springframework.web.servlet.resource.NoResourceFoundException exception = new org.springframework.web.servlet.resource.NoResourceFoundException(
                    org.springframework.http.HttpMethod.GET, "/.well-known/appspecific", "resource");

            ProblemDetail result = exceptionHandler.handleNoResourceFound(exception);

            assertEquals(404, result.getStatus());
            assertEquals("No static resource resource for request '/.well-known/appspecific'.", result.getDetail());
            assertEquals("Not Found", result.getTitle());
            assertEquals(URI.create("about:blank"), result.getType());
        }
    }

    @Nested
    @DisplayName("handleGenericException")
    class HandleGenericException {

        @Test
        @DisplayName("should return ProblemDetail with INTERNAL_SERVER_ERROR status")
        void shouldReturnProblemDetailWithInternalServerErrorStatus() {
            Exception exception = new Exception("Something went wrong");

            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            assertEquals(500, result.getStatus());
            assertEquals("An unexpected error occurred", result.getDetail());
            assertEquals("Internal Server Error", result.getTitle());
            assertEquals(URI.create("about:blank"), result.getType());
        }

        @Test
        @DisplayName("should not expose internal exception message")
        void shouldNotExposeInternalExceptionMessage() {
            Exception exception = new Exception("Database connection failed with password=secret123");

            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            assertEquals("An unexpected error occurred", result.getDetail());
            assertFalse(result.getDetail().contains("password"));
        }

        @Test
        @DisplayName("should handle NullPointerException")
        void shouldHandleNullPointerException() {
            NullPointerException exception = new NullPointerException("Object was null");

            ProblemDetail result = exceptionHandler.handleGenericException(exception);

            assertEquals(500, result.getStatus());
            assertEquals("An unexpected error occurred", result.getDetail());
        }
    }
}
