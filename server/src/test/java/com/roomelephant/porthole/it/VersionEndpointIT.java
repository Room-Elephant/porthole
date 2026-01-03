package com.roomelephant.porthole.it;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static wiremock.org.eclipse.jetty.util.TypeUtil.isFalse;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.Container;

class VersionEndpointIT extends IntegrationTestBase {

    @BeforeEach
    void setupAuthStub() {
        // Only VersionEndpointIT needs auth endpoint stub for registry authentication
        wireMock.stubFor(get(urlMatching("/auth.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"mock-token\"}")));
    }

    @Test
    void shouldReturnUpdateAvailableWhenNewerVersionExists() {
        // Using the container ID from IntegrationTestBase
        assertThat(alpineContainerId).isNotNull();

        // Stub registry endpoints using helper methods
        stubRegistryTags("busybox", "1.35.0", "1.36.1");
        stubManifestDigest("busybox", "1.36.1", "sha256:newdigest");
        stubManifestDigest("busybox", "1.35.0", "sha256:olddigest"); // Current version

        ResponseEntity<String> response = restTemplate.getForEntity(
                createURLWithPort("/api/containers/" + alpineContainerId + "/version"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(OK.value());
        String responseBody = response.getBody();

        assertThat((String) JsonPath.read(responseBody, "$.currentVersion")).isEqualTo("1.35.0");
        assertThat((String) JsonPath.read(responseBody, "$.latestVersion")).isEqualTo("1.36.1");
        isTrue(JsonPath.read(responseBody, "$.updateAvailable"));
    }

    @Test
    void shouldReturnNoUpdateForLocalImage() throws Exception {
        // 1. Build a local image inside DinD so it has no RepoDigests
        // We reuse the busybox image already pulled
        docker.execInContainer("sh", "-c", "echo 'FROM busybox:1.35.0' > Dockerfile.local");
        Container.ExecResult buildResult =
                docker.execInContainer("docker", "build", "-f", "Dockerfile.local", "-t", "my-local-image:1.0", ".");
        if (buildResult.getExitCode() != 0) {
            throw new RuntimeException("Failed to build local image: " + buildResult.getStderr());
        }

        // 2. Run a container from this local image
        Container.ExecResult runResult = docker.execInContainer(
                "docker", "run", "-d", "--name", "local-container", "my-local-image:1.0", "sleep", "3600");
        if (runResult.getExitCode() != 0) {
            throw new RuntimeException("Failed to run local container: " + runResult.getStderr());
        }
        String localContainerId = runResult.getStdout().trim();

        try {
            // 3. Query the version endpoint
            // Note: This test intentionally does NOT stub registry endpoints for
            // my-local-image
            // The service will get 404s when trying to fetch tags/manifest, which is
            // correct behavior
            // for local-only images. The test verifies latestVersion=null and
            // updateAvailable=false
            ResponseEntity<String> response = restTemplate.getForEntity(
                    createURLWithPort("/api/containers/" + localContainerId + "/version"), String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(OK.value());
            String responseBody = response.getBody();

            // 4. Verify results
            // Current version should be detected (from tag 1.0)
            // Latest version should be null (no remote to check)
            // Update available should be false
            assertThat((String) JsonPath.read(responseBody, "$.currentVersion")).isEqualTo("1.0");
            assertThat((String) JsonPath.read(responseBody, "$.latestVersion")).isNull();
            isFalse(JsonPath.read(responseBody, "$.updateAvailable"));
        } finally {
            // Cleanup
            docker.execInContainer("docker", "rm", "-f", "local-container");
            docker.execInContainer("docker", "rmi", "my-local-image:1.0");
        }
    }

    @Test
    void shouldReturn404WhenContainerNotFound() {
        String nonExistentId = "non-existent-id";

        ResponseEntity<String> response = restTemplate.getForEntity(
                createURLWithPort("/api/containers/" + nonExistentId + "/version"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
