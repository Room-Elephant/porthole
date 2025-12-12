package com.roomelephant.porthole.config;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerHostConfig;
import com.github.dockerjava.api.model.ContainerMount;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM native image configuration for reflection hints.
 * Registers docker-java model classes that require reflection for JSON serialization.
 */
@Configuration
@ImportRuntimeHints(NativeHintsConfig.DockerJavaHints.class)
public class NativeHintsConfig {

    static class DockerJavaHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Register docker-java model classes for reflection
            hints.reflection()
                    .registerType(Container.class, MemberCategory.values())
                    .registerType(ContainerPort.class, MemberCategory.values())
                    .registerType(ContainerHostConfig.class, MemberCategory.values())
                    .registerType(ContainerMount.class, MemberCategory.values())
                    .registerType(ContainerNetwork.class, MemberCategory.values())
                    .registerType(ContainerNetworkSettings.class, MemberCategory.values());
        }
    }
}

