package com.roomelephant.porthole.it.infra;

import com.github.dockerjava.api.DockerClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class DockerConfiguration {

    @Bean(destroyMethod = "close")
    DockerInfrastructure dockerInfrastructure() {
        return new DockerInfrastructure();
    }

    @Bean
    DockerClient dockerTestcontainersClient(DockerInfrastructure dockerInfrastructure) {
        return dockerInfrastructure.getDetailsDockerClient();
    }
}
