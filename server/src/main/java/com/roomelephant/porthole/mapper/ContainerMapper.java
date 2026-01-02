package com.roomelephant.porthole.mapper;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.roomelephant.porthole.component.IconComponent;
import com.roomelephant.porthole.model.ContainerDTO;
import com.roomelephant.porthole.util.ImageUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class ContainerMapper {

    private static final String UNKNOWN = "Unknown";
    private static final String LABEL_PROJECT = "com.docker.compose.project";

    private final IconComponent iconComponent;

    public ContainerMapper(IconComponent iconComponent) {
        this.iconComponent = iconComponent;
    }

    public @NonNull ContainerDTO toDTO(@NonNull Container container) {
        String name = container.getNames().length > 0 ? container.getNames()[0].substring(1) : UNKNOWN;
        String imageFull = container.getImage();
        Set<Integer> ports = container.getPorts() != null
                ? Arrays.stream(container.getPorts())
                        .map(ContainerPort::getPublicPort)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
                : Set.of();

        String iconUrl = resolveIconUrl(imageFull);

        Map<String, String> labels = container.getLabels();
        String project = labels != null ? labels.get(LABEL_PROJECT) : null;

        String state = container.getState();
        String status = container.getStatus();

        String displayName = computeDisplayName(name, project);

        return new ContainerDTO(
                container.getId(),
                name,
                displayName,
                imageFull,
                ports,
                iconUrl,
                project,
                state,
                status);
    }

    private String resolveIconUrl(String imageFull) {
        return iconComponent.resolveIcon(ImageUtils.extractName(imageFull));
    }

    private String computeDisplayName(String name, String project) {
        if (project != null && name.startsWith(project + "-")) {
            return name.substring(project.length() + 1);
        }
        return name;
    }
}
