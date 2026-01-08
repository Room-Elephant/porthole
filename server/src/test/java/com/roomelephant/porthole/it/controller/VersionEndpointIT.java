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
        stubRegistryTags("busybox", "1.37.0-uclibc", "1.36.1");
        stubManifestDigest("busybox", "1.36.1", "sha256:newdigest");
        stubManifestDigest("busybox", "1.37.0-uclibc", "sha256:currentdiggest");

        VersionDTO response = fetchVersion(testAppContainer.getContainerId());

        assertThat(response.currentVersion()).isEqualTo("1.37.0-uclibc");
        assertThat(response.latestVersion()).isEqualTo("1.36.1");
        assertThat(response.updateAvailable()).isTrue();
    }

    @Test
    void shouldReturnNoUpdateForLocalImage() {
        stubRegistryTags404("my-local-image");

        VersionDTO response = fetchVersion(localContainer.getContainerId());

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
                tagsJson.append(",");
            }
            tagsJson.append("{\"name\": \"").append(versions[i]).append("\"}");
        }
        tagsJson.append("]}");

        wireMock.stubFor(get(urlEqualTo("/v2/repositories/library/" + image + "/tags?page_size=100"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(tagsJson.toString())));
    }

    void stubRegistryTags404(String image) {
        wireMock.stubFor(get(urlEqualTo("/v2/repositories/library/" + image + "/tags?page_size=100"))
                .willReturn(aResponse().withStatus(404)));
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
        ResponseEntity<VersionDTO> response = fetch("/api/containers/" + containerId + "/version", VersionDTO.class);
        assertThat(response.getStatusCode()).isEqualTo(OK);

        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }
}
