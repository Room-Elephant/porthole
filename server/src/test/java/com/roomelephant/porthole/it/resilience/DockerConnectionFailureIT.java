package com.roomelephant.porthole.it.resilience;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@org.springframework.test.context.ActiveProfiles("it")
class DockerConnectionFailureIT {

    private static File socketFile;

    @LocalServerPort
    protected int port;

    private RestTemplate restTemplate;

    @BeforeEach
    void setup() {
        this.restTemplate = new RestTemplate();
        this.restTemplate.setErrorHandler(new ResponseErrorHandler() {
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            public void handleError(ClientHttpResponse response) {}
        });
    }

    protected @NotNull String createURLWithPort(String uri) {
        return "http://localhost:" + port + uri;
    }

    @DynamicPropertySource
    static void configureProperties(@NotNull DynamicPropertyRegistry registry) {
        try {
            // Create a dummy file path for the Unix socket
            socketFile = File.createTempFile("docker-failure-test", ".sock");
            socketFile.delete(); // Ensure it starts non-existent

            // Point porthole.docker.host to this file path
            registry.add("porthole.docker.host", () -> "unix://" + socketFile.getAbsolutePath());

            // Dummy registry properties to satisfy startup requirements
            registry.add("registry.urls.registry", () -> "http://localhost:9999/v2/");
            registry.add("registry.urls.auth", () -> "http://localhost:9999/auth");
            registry.add("registry.urls.repositories", () -> "http://localhost:9999/v2/repositories/");
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup socket file path", e);
        }
    }

    @AfterAll
    static void cleanup() {
        if (socketFile != null && socketFile.exists()) {
            socketFile.delete();
        }
    }

    // --- Helpers ---

    private void ensureSocketMissing() {
        if (socketFile.exists()) {
            socketFile.delete();
        }
    }

    private void ensureSocketPermissionDenied() throws IOException {
        if (!socketFile.exists()) {
            socketFile.createNewFile();
        }
        Set<PosixFilePermission> perms = new HashSet<>(); // Empty set = 000 permissions
        Files.setPosixFilePermissions(socketFile.toPath(), perms);
    }

    // --- Health Endpoint Tests ---

    @Test
    void health_ShouldReturnDown_WhenSocketMissing() {
        ensureSocketMissing();

        ResponseEntity<String> response =
                restTemplate.getForEntity(createURLWithPort("/actuator/health"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).contains("\"status\":\"DOWN\"");
        assertThat(response.getBody()).contains("\"docker\":{\"status\":\"DOWN\"}");
    }

    @Test
    void health_ShouldReturnDown_WhenPermissionDenied() throws IOException {
        try {
            ensureSocketPermissionDenied();

            ResponseEntity<String> response =
                    restTemplate.getForEntity(createURLWithPort("/actuator/health"), String.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
            assertThat(response.getBody()).contains("\"status\":\"DOWN\"");
            assertThat(response.getBody()).contains("\"docker\":{\"status\":\"DOWN\"}");
        } finally {
            ensureSocketMissing(); // Cleanup
        }
    }

    // --- Containers Endpoint Tests ---

    @Test
    void containers_ShouldReturn500_WhenSocketMissing() {
        ensureSocketMissing();

        ResponseEntity<String> response = restTemplate.getForEntity(createURLWithPort("/api/containers"), String.class);

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void containers_ShouldReturn500_WhenPermissionDenied() throws IOException {
        try {
            ensureSocketPermissionDenied();

            ResponseEntity<String> response =
                    restTemplate.getForEntity(createURLWithPort("/api/containers"), String.class);

            assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        } finally {
            ensureSocketMissing(); // Cleanup
        }
    }

    // --- Version Endpoint Tests ---

    @Test
    void version_ShouldReturn500_WhenSocketMissing() {
        ensureSocketMissing();

        String containerId = "random-id";
        ResponseEntity<String> response = restTemplate.getForEntity(
                createURLWithPort("/api/containers/" + containerId + "/version"), String.class);

        assertThat(response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void version_ShouldReturn500_WhenPermissionDenied() throws IOException {
        try {
            ensureSocketPermissionDenied();

            String containerId = "random-id";
            ResponseEntity<String> response = restTemplate.getForEntity(
                    createURLWithPort("/api/containers/" + containerId + "/version"), String.class);

            assertThat(response.getStatusCode().is5xxServerError()).isTrue();
        } finally {
            ensureSocketMissing(); // Cleanup
        }
    }
}
