package com.roomelephant.porthole.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ContainersEndpointIT extends IntegrationTestBase {

    @Test
    void shouldReturnRunningContainers() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                createURLWithPort("/api/containers?includeWithoutPorts=true&includeStopped=true"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody()).contains(TEST_APP_CONTAINER_NAME);
        assertThat(response.getBody()).contains("busybox");
        assertThat(response.getBody()).contains("running");
    }

    @Test
    void shouldReturnEmptyListWhenNoContainers() throws Exception {
        // Temporarily stop all test containers to test empty list
        docker.execInContainer("docker", "stop", TEST_APP_CONTAINER_NAME);
        docker.execInContainer("docker", "stop", TEST_ALPINE_CONTAINER_NAME);

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    createURLWithPort("/api/containers?includeWithoutPorts=true&includeStopped=false"), String.class);

            assertThat(response.getStatusCode()).isEqualTo(OK);
            // Should be empty array since we stopped all running containers
            assertThat(response.getBody()).isEqualTo("[]");
        } finally {
            // Restart containers for other tests
            docker.execInContainer("docker", "start", TEST_APP_CONTAINER_NAME);
            docker.execInContainer("docker", "start", TEST_ALPINE_CONTAINER_NAME);
        }
    }

    @Test
    void shouldFilterStoppedContainers() throws Exception {
        // 1. Create a stopped container
        docker.execInContainer("docker", "create", "--name", "stopped-container", "busybox:1.35.0");

        try {
            // 2. Query with includeStopped=false (default)
            ResponseEntity<String> response =
                    restTemplate.getForEntity(createURLWithPort("/api/containers?includeStopped=false"), String.class);

            assertThat(response.getBody()).contains(TEST_APP_CONTAINER_NAME); // Running
            assertThat(response.getBody()).doesNotContain("stopped-container"); // Stopped

            // 3. Query with includeStopped=true
            response = restTemplate.getForEntity(
                    createURLWithPort("/api/containers?includeStopped=true&includeWithoutPorts=true"), String.class);

            assertThat(response.getBody()).contains(TEST_APP_CONTAINER_NAME);
            assertThat(response.getBody()).contains("stopped-container");
        } finally {
            docker.execInContainer("docker", "rm", "-f", "stopped-container");
        }
    }

    @Test
    void shouldFilterContainersWithoutPorts() throws Exception {
        // 1. Run a container without publishing ports
        docker.execInContainer(
                "docker", "run", "-d", "--name", "no-ports-container", "busybox:1.35.0", "sleep", "3600");

        try {
            // 2. Query with includeWithoutPorts=false (default)
            ResponseEntity<String> response = restTemplate.getForEntity(
                    createURLWithPort("/api/containers?includeWithoutPorts=false"), String.class);

            assertThat(response.getBody()).contains(TEST_APP_CONTAINER_NAME); // Has ports
            assertThat(response.getBody()).doesNotContain("no-ports-container"); // No ports

            // 3. Query with includeWithoutPorts=true
            response = restTemplate.getForEntity(
                    createURLWithPort("/api/containers?includeWithoutPorts=true"), String.class);

            assertThat(response.getBody()).contains(TEST_APP_CONTAINER_NAME);
            assertThat(response.getBody()).contains("no-ports-container");
        } finally {
            docker.execInContainer("docker", "rm", "-f", "no-ports-container");
        }
    }
}
