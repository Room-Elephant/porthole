package com.roomelephant.porthole.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "registry")
@Validated
public record RegistryProperties(
        @Valid
        @NotNull(message = "Timeout configuration is required")
        Timeout timeout,

        @Valid
        @NotNull(message = "Cache configuration is required")
        Cache cache
) {
    public record Timeout(
            @NotNull(message = "Connect timeout must be configured")
            Duration connect,

            @NotNull(message = "Read timeout must be configured")
            Duration read
    ) {}

    public record Cache(
            @NotNull(message = "Cache TTL must be configured")
            Duration ttl,

            @Positive(message = "Version cache size must be positive")
            int versionMaxSize
    ) {}
}

