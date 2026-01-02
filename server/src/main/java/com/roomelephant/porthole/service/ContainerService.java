package com.roomelephant.porthole.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.roomelephant.porthole.mapper.ContainerMapper;
import com.roomelephant.porthole.model.ContainerDTO;
import java.net.SocketException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ContainerService {

    private final DockerClient dockerClient;
    private final ContainerMapper containerMapper;

    public ContainerService(DockerClient dockerClient, ContainerMapper containerMapper) {
        this.dockerClient = dockerClient;
        this.containerMapper = containerMapper;
    }

    public @NonNull List<ContainerDTO> getContainers(boolean includeWithoutPorts, boolean includeStopped) {
        List<Container> containers;
        try {
            containers = dockerClient.listContainersCmd()
                    .withShowAll(includeStopped)
                    .exec();
        } catch (RuntimeException e) {
            if (isDockerConnectionError(e)) {
                log.warn("Docker connection failed", e);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Docker is not reachable", e);
            }
            throw e;
        }

        return containers.stream()
                .map(containerMapper::toDTO)
                .filter(dto -> includeWithoutPorts || dto.hasPublicPorts())
                .toList();
    }

    private boolean isDockerConnectionError(RuntimeException e) {
        return e.getCause() instanceof SocketException;
    }
}
