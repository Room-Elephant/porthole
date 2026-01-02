package com.roomelephant.porthole.config;

import com.github.dockerjava.api.DockerClient;
import java.net.SocketException;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class DockerHealthIndicator implements HealthIndicator {

    private final DockerClient dockerClient;

    public DockerHealthIndicator(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public Health health() {
        try {
            dockerClient.pingCmd().exec();
            return Health.up().build();
        } catch (RuntimeException e) {
            if (isDockerConnectionError(e)) {
                return Health.down()
                        .withDetail("Error connecting to docker", e.getCause().getMessage())
                        .build();
            }
            return Health.down()
                    .withDetail("Unexpected exception", e.getMessage())
                    .build();
        }
    }

    private boolean isDockerConnectionError(RuntimeException e) {
        return e.getCause() instanceof SocketException;
    }
}
