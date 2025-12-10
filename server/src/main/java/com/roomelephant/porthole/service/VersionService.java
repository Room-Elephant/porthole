package com.roomelephant.porthole.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.roomelephant.porthole.model.VersionDTO;
import com.roomelephant.porthole.util.ImageUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

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
        Container container = findContainerById(containerId);
        if (container == null) {
            return new VersionDTO(null, null, false);
        }

        String imageFull = container.getImage();

        // Get repoDigests to detect local images (no RepoDigests = locally built)
        List<String> repoDigests = getRepoDigests(container);
        boolean isLocalImage = repoDigests == null || repoDigests.isEmpty();

        String currentVersion = getVersionFromContainer(container);

        if (isLocalImage) {
            return new VersionDTO(currentVersion, null, false);
        }

        String latestVersion = registryService.getLatestVersion(imageFull);
        boolean updateAvailable = checkForUpdate(container, currentVersion, latestVersion, repoDigests);

        return new VersionDTO(currentVersion, latestVersion, updateAvailable);
    }

    private @Nullable Container findContainerById(@NonNull String containerId) {
        try {
            List<Container> containers = dockerClient.listContainersCmd()
                    .withShowAll(true)
                    .exec();
            return containers.stream()
                    .filter(c -> c.getId().equals(containerId))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private @Nullable List<String> getRepoDigests(@NonNull Container container) {
        try {
            var inspectImage = dockerClient.inspectImageCmd(container.getImageId()).exec();
            return inspectImage.getRepoDigests();
        } catch (Exception e) {
            return null;
        }
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
        } catch (Exception e) {
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

    private boolean checkForUpdate(@NonNull Container container, @Nullable String currentVersion, @Nullable String latestVersion, @NonNull List<String> repoDigests) {
        String imageFull = container.getImage();
        String tag = ImageUtils.extractTag(imageFull);

        try {
            String remoteDigest = registryService.getDigest(imageFull, tag);
            if (remoteDigest != null) {
                boolean match = repoDigests.stream().anyMatch(rd -> rd.contains(remoteDigest));
                if (!match) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Squelch errors to avoid breaking UI
        }

        if (ImageUtils.isSemver(tag) && currentVersion != null && latestVersion != null) {
            return !currentVersion.equals(latestVersion);
        }

        return false;
    }
}

