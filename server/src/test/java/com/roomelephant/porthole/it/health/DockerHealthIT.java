package com.roomelephant.porthole.it.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.roomelephant.porthole.it.infra.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class DockerHealthIT extends IntegrationTestBase {

  @Test
  void shouldReturnUpWhenDockerIsReachableAndRequestOnlyDockerComponent() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(createURLWithPort("/actuator/health/docker"), String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(OK.value());
    assertThat(response.getBody()).contains("{\"status\":\"UP\"}");
  }

    @Test
    void shouldReturnUpWhenDockerIsReachable() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(createURLWithPort("/actuator/health"), String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(OK.value());
        assertThat(response.getBody()).contains("\"status\":\"UP\"}");
        assertThat(response.getBody()).contains("\"docker\":{\"status\":\"UP\"}");
    }

    @Test
    void shouldReturnDownWhenDockerIsUnreachable() {
        pauseDocker();

        try {
            ResponseEntity<String> response =
                    restTemplate.getForEntity(createURLWithPort("/actuator/health"), String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(SERVICE_UNAVAILABLE.value());
            assertThat(response.getBody()).contains("\"status\":\"DOWN\"");
            assertThat(response.getBody()).contains("\"docker\":{\"status\":\"DOWN\"}");
        } finally {
            unpauseDocker();
        }
    }
}
