package com.roomelephant.porthole.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "docker")
@Validated
public record DockerProperties(
        @NotBlank(message = "Docker host must be configured")
        String host,

        @jakarta.validation.constraints.NotNull(message = "Connection timeout must be configured")
        java.time.Duration connectionTimeout,

        @jakarta.validation.constraints.NotNull(message = "Response timeout must be configured")
        java.time.Duration responseTimeout) {}
