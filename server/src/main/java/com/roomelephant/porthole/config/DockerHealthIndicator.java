package com.roomelephant.porthole.config;

import com.github.dockerjava.api.DockerClient;
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
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
