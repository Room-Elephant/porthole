package com.roomelephant.porthole.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerConfig;
import com.roomelephant.porthole.component.RegistryService;
import com.roomelephant.porthole.model.VersionDTO;
import com.roomelephant.porthole.util.ImageUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class VersionService {

    private static final String LABEL_OCI_IMAGE_VERSION = "org.opencontainers.image.version";
    private static final String LABEL_IMAGE_VERSION = "version";

    private final DockerClient dockerClient;
    private final RegistryService registryService;

    public VersionService(DockerClient dockerClient, RegistryService registryService) {
        this.dockerClient = dockerClient;
        this.registryService = registryService;
    }

    public @NonNull VersionDTO getVersionInfo(@NonNull String containerId) {
        InspectContainerResponse container;
        try {
            container = dockerClient.inspectContainerCmd(containerId).exec();
        } catch (Exception _) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found: " + containerId);
        }

        var config = container.getConfig();
        if (config == null || config.getImage() == null) {
            return new VersionDTO(null, null, false);
        }

        String imageFull = config.getImage();

        List<String> repoDigests = getRepoDigests(container.getImageId());
        boolean isLocalImage = repoDigests == null || repoDigests.isEmpty();

        String currentVersion = getVersionFromContainer(config, imageFull);

        if (isLocalImage) {
            return new VersionDTO(currentVersion, null, false);
        }

        String latestVersion = registryService.getLatestVersion(imageFull);
        boolean updateAvailable = checkForUpdate(imageFull, currentVersion, latestVersion, repoDigests);

        return new VersionDTO(currentVersion, latestVersion, updateAvailable);
    }

    private @Nullable List<String> getRepoDigests(@NonNull String imageId) {
        try {
            var inspectImage = dockerClient.inspectImageCmd(imageId).exec();
            return inspectImage.getRepoDigests();
        } catch (Exception _) {
            return null;
        }
    }

    private String getVersionFromContainer(@NonNull ContainerConfig config, @NonNull String imageFull) {
        String imageName = ImageUtils.extractName(imageFull);

        String envVersion = getVersionFromEnvVars(config.getEnv(), imageName);
        if (envVersion != null) {
            return envVersion;
        }

        String labelVersion = getVersionFromLabels(config.getLabels());
        if (labelVersion != null) {
            return labelVersion;
        }

        return ImageUtils.extractTag(imageFull);
    }

    private String getVersionFromEnvVars(String @Nullable [] envs, @NonNull String imageName) {
        if (envs == null) {
            return null;
        }

        String targetEnv = imageName.toUpperCase().replaceAll("[^A-Z0-9]", "_") + "_VERSION=";

        for (String env : envs) {
            if (env.startsWith(targetEnv)) {
                String[] parts = env.split("=", 2);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    return parts[1];
                }
            }
            if (env.startsWith("VERSION=")) {
                String[] parts = env.split("=", 2);
                if (parts.length > 1 && !parts[1].isEmpty()) {
                    return parts[1];
                }
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

    private boolean checkForUpdate(@NonNull String imageFull, @Nullable String currentVersion, @Nullable String latestVersion, @NonNull List<String> repoDigests) {
        String tag = ImageUtils.extractTag(imageFull);

        try {
            String remoteDigest = registryService.getDigest(imageFull, tag);
            if (remoteDigest != null) {
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
}

