package com.roomelephant.porthole.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class DockerHealthIT extends IntegrationTestBase {

    @Test
    void shouldReturnUpWhenDockerIsReachable() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(createURLWithPort("/actuator/health"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(OK.value());
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
        assertThat(response.getBody()).contains("\"docker\":{\"status\":\"UP\"}");
    }

    @Test
    void shouldReturnDownWhenDockerIsUnreachable() {
        // Pause the container to simulate network unreachability/daemon freeze
        docker.getDockerClient().pauseContainerCmd(docker.getContainerId()).exec();
        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(createURLWithPort("/actuator/health"), String.class);

            // Health endpoint usually returns 503 SERVICE_UNAVAILABLE when DOWN
            assertThat(response.getStatusCode().value()).isEqualTo(SERVICE_UNAVAILABLE.value());
            assertThat(response.getBody()).contains("\"status\":\"DOWN\"");
            assertThat(response.getBody()).contains("\"docker\":{\"status\":\"DOWN\"}");
        } finally {
            // Always unpause to avoid breaking subsequent tests
            docker.getDockerClient()
                    .unpauseContainerCmd(docker.getContainerId())
                    .exec();
        }
    }
}
