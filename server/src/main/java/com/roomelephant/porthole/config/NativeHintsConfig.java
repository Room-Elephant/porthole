package com.roomelephant.porthole.config;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerHostConfig;
import com.github.dockerjava.api.model.ContainerMount;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ContainerNetworkSettings;
import com.github.dockerjava.api.model.ContainerPort;
import com.roomelephant.porthole.config.properties.DashboardProperties;
import com.roomelephant.porthole.config.properties.DockerProperties;
import com.roomelephant.porthole.config.properties.RegistryProperties;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/**
 * GraalVM native image configuration for reflection hints.
 * Registers classes that require reflection for native image compatibility.
 */
@Configuration
@ImportRuntimeHints(NativeHintsConfig.NativeHints.class)
public class NativeHintsConfig {

    static class NativeHints implements RuntimeHintsRegistrar {

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

            // Register configuration properties for Hibernate Validator reflection
            hints.reflection()
                    .registerType(DashboardProperties.class, MemberCategory.values())
                    .registerType(DashboardProperties.Icons.class, MemberCategory.values())
                    .registerType(DockerProperties.class, MemberCategory.values())
                    .registerType(RegistryProperties.class, MemberCategory.values())
                    .registerType(RegistryProperties.Timeout.class, MemberCategory.values())
                    .registerType(RegistryProperties.Cache.class, MemberCategory.values());
        }
    }
}

