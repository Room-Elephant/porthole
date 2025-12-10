package com.roomelephant.porthole.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.roomelephant.porthole.model.ContainerDTO;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DockerService {

    private final DockerClient dockerClient;
    private final ContainerMapper containerMapper;

    public DockerService(DockerClient dockerClient, ContainerMapper containerMapper) {
        this.dockerClient = dockerClient;
        this.containerMapper = containerMapper;
    }

    public @NonNull List<ContainerDTO> getContainers(boolean includeWithoutPorts, boolean includeStopped) {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(includeStopped)
                .exec();

        return containers.parallelStream()
                .map(containerMapper::toDTO)
                .filter(dto -> includeWithoutPorts || dto.hasPublicPorts())
                .toList();
    }
}
