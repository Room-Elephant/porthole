package com.roomelephant.porthole.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "docker")
@Validated
public record DockerProperties(
        @NotBlank(message = "Docker host must be configured")
        String host
) {}

