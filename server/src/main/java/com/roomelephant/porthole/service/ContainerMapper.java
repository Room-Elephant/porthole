package com.roomelephant.porthole.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import com.roomelephant.porthole.config.IconConfigService;
import com.roomelephant.porthole.model.ContainerDTO;
import com.roomelephant.porthole.util.ImageUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ContainerMapper {

    private static final String UNKNOWN = "Unknown";
    private static final String LABEL_PROJECT = "com.docker.compose.project";
    private static final String LABEL_OCI_IMAGE_VERSION = "org.opencontainers.image.version";
    private static final String LABEL_IMAGE_VERSION = "version";

    private final DockerClient dockerClient;
    private final IconConfigService iconConfigService;
    private final RegistryService registryService;

    public ContainerMapper(DockerClient dockerClient, IconConfigService iconConfigService, RegistryService registryService) {
        this.dockerClient = dockerClient;
        this.iconConfigService = iconConfigService;
        this.registryService = registryService;
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

        String currentVersion = getVersionFromContainer(container);
        String latestVersion = registryService.getLatestVersion(imageFull);
        boolean updateAvailable = checkForUpdate(container, currentVersion, latestVersion);

        boolean hasPublicPorts = !ports.isEmpty();

        return new ContainerDTO(
                container.getId(),
                name,
                imageFull,
                ports,
                iconUrl,
                currentVersion,
                latestVersion,
                updateAvailable,
                project,
                hasPublicPorts);
    }

    private String getVersionFromContainer(@NonNull Container container) {
        try {
            var inspect = dockerClient.inspectContainerCmd(container.getId()).exec();
            String imageName = ImageUtils.extractName(container.getImage());

            String envVersion = getVersionFromEnvVars(inspect.getConfig().getEnv(), imageName);
            if (envVersion != null) {
                return envVersion;
            }

            String labelVersion = getVersionFromLabels(inspect.getConfig().getLabels());
            if (labelVersion != null) {
                return labelVersion;
            }
        } catch (Exception _) {
            // Ignore inspect failures
        }

        return ImageUtils.extractTag(container.getImage());
    }

    private String getVersionFromEnvVars(String @Nullable [] envs, @NonNull String imageName) {
        if (envs == null) {
            return null;
        }

        String targetEnv = imageName.toUpperCase().replaceAll("[^A-Z0-9]", "_") + "_VERSION=";

        for (String env : envs) {
            if (env.startsWith(targetEnv)) {
                return env.split("=")[1];
            }
            if (env.startsWith("VERSION=")) {
                return env.split("=")[1];
            }
        }
        return null;
    }

    private String getVersionFromLabels(@Nullable Map<String, String> labels) {
        if (labels == null) {
            return null;
        }
        if (labels.containsKey(LABEL_OCI_IMAGE_VERSION)) {
            return labels.get(LABEL_OCI_IMAGE_VERSION);
        }
        if (labels.containsKey(LABEL_IMAGE_VERSION)) {
            return labels.get(LABEL_IMAGE_VERSION);
        }
        return null;
    }

    private boolean checkForUpdate(@NonNull Container container, @Nullable String currentVersion, @Nullable String latestVersion) {
        String imageFull = container.getImage();
        String tag = ImageUtils.extractTag(imageFull);

        try {
            var inspectImage = dockerClient.inspectImageCmd(container.getImageId()).exec();
            List<String> repoDigests = inspectImage.getRepoDigests();

            String remoteDigest = registryService.getDigest(imageFull, tag);

            if (remoteDigest != null && repoDigests != null) {
                boolean match = repoDigests.stream().anyMatch(rd -> rd.contains(remoteDigest));
                if (!match) {
                    return true;
                }
            }
        } catch (Exception _) {
            // Squelch errors to avoid breaking UI
        }

        if (ImageUtils.isSemver(tag) && currentVersion != null && latestVersion != null) {
            return !currentVersion.equals(latestVersion);
        }

        return false;
    }

    private String resolveIconUrl(String imageFull) {
        return iconConfigService.resolveIcon(ImageUtils.extractName(imageFull));
    }
}

