package com.roomelephant.porthole.service;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.roomelephant.porthole.config.IconConfigService;
import com.roomelephant.porthole.model.ContainerDTO;
import com.roomelephant.porthole.util.ImageUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ContainerMapper {

    private static final String UNKNOWN = "Unknown";
    private static final String LABEL_PROJECT = "com.docker.compose.project";

    private final IconConfigService iconConfigService;

    public ContainerMapper(IconConfigService iconConfigService) {
        this.iconConfigService = iconConfigService;
    }

    public @NonNull ContainerDTO toDTO(@NonNull Container container) {
        String name = container.getNames().length > 0 ? container.getNames()[0].substring(1) : UNKNOWN;
        String imageFull = container.getImage();
        List<Integer> ports = Arrays.stream(container.getPorts())
                .map(ContainerPort::getPublicPort)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        String iconUrl = resolveIconUrl(imageFull);

        Map<String, String> labels = container.getLabels();
        String project = labels != null ? labels.get(LABEL_PROJECT) : null;

        boolean hasPublicPorts = !ports.isEmpty();

        String state = container.getState();
        String status = container.getStatus();

        return new ContainerDTO(
                container.getId(),
                name,
                imageFull,
                ports,
                iconUrl,
                project,
                hasPublicPorts,
                state,
                status);
    }

    private String resolveIconUrl(String imageFull) {
        return iconConfigService.resolveIcon(ImageUtils.extractName(imageFull));
    }
}

