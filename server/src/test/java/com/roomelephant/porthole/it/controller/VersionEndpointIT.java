package com.roomelephant.porthole.it.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.head;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import com.roomelephant.porthole.domain.model.VersionDTO;
import com.roomelephant.porthole.it.infra.IntegrationTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class VersionEndpointIT extends IntegrationTestBase {

    @BeforeEach
    void setupAuthStub() {
        wireMock.stubFor(get(urlMatching("/auth.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"mock-token\"}")));
    }

    @Test
    void shouldReturnUpdateAvailableWhenNewerVersionExists() {
        stubRegistryTags("busybox", "1.35.0", "1.36.1");
        stubManifestDigest("busybox", "1.36.1", "sha256:newdigest");
        stubManifestDigest("busybox", "1.35.0", "sha256:currentdiggest");

        VersionDTO response = fetchVersion(dockerInfra.getTestAppContainer().getContainerId());

        assertThat(response.currentVersion()).isEqualTo("1.35.0");
        assertThat(response.latestVersion()).isEqualTo("1.36.1");
        assertThat(response.updateAvailable()).isTrue();
    }

    @Test
    void shouldReturnNoUpdateForLocalImage() {
        // The localContainer is already running a local-only image (my-local-image:1.0)
        // See IntegrationTestBase.createAndStartContainers()

        // Query the version endpoint for the local container
        // Note: This test intentionally does NOT stub registry endpoints for
        // my-local-image
        // The service will get 404s when trying to fetch tags/manifest, which is
        // correct behavior
        // for local-only images. The test verifies latestVersion=null and
        // updateAvailable=false
        VersionDTO response = fetchVersion(dockerInfra.getLocalContainer().getContainerId());

        // Verify results
        // Current version should be detected (from tag 1.0)
        // Latest version should be null (no remote to check)
        // Update available should be false
        assertThat(response.currentVersion()).isEqualTo("1.0");
        assertThat(response.latestVersion()).isNull();
        assertThat(response.updateAvailable()).isFalse();
    }

    @Test
    void shouldReturn404WhenContainerNotFound() {
        String nonExistentId = "non-existent-id";

        ResponseEntity<String> response = fetch("/api/containers/" + nonExistentId + "/version");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // WireMock helper methods to reduce verbosity

    /**
     * Stub Docker registry tags endpoint with given versions
     *
     * @param image    Image name (e.g., "busybox")
     * @param versions Array of version strings (e.g., "1.35.0", "1.36.1")
     */
    void stubRegistryTags(String image, String... versions) {
        StringBuilder tagsJson = new StringBuilder("{\"results\": [");
        for (int i = 0; i < versions.length; i++) {
            if (i > 0) {
                tagsJson.append(", ");
            }
            tagsJson.append("{\"name\": \"").append(versions[i]).append("\"}");
        }
        tagsJson.append("]}");

        wireMock.stubFor(get(urlEqualTo("/v2/repositories/library/" + image + "/tags?page_size=100"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(tagsJson.toString())));
    }

    /**
     * Stub Docker registry manifest HEAD request with digest
     *
     * @param image   Image name (e.g., "busybox")
     * @param version Version tag (e.g., "1.36.1")
     * @param digest  Digest value (e.g., "sha256:newdigest")
     */
    void stubManifestDigest(String image, String version, String digest) {
        wireMock.stubFor(head(urlEqualTo("/v2/library/" + image + "/manifests/" + version))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/vnd.docker.distribution.manifest.v2+json")
                        .withHeader("Docker-Content-Digest", digest)
                        .withBody("{}")));
    }

    protected @NotNull VersionDTO fetchVersion(String containerId) {
        ResponseEntity<@NotNull VersionDTO> response =
                fetch("/api/containers/" + containerId + "/version", VersionDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }
}
