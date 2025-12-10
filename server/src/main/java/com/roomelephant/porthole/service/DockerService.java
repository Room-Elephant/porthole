package com.roomelephant.porthole.service;

import com.roomelephant.porthole.config.IconConfigService;
import com.roomelephant.porthole.model.ContainerDTO;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerPort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DockerService {

    private final DockerClient dockerClient;

    private final IconConfigService iconConfigService;
    private final RegistryService registryService;

    public DockerService(DockerClient dockerClient, IconConfigService iconConfigService,
            RegistryService registryService) {
        this.dockerClient = dockerClient;
        this.iconConfigService = iconConfigService;
        this.registryService = registryService;
    }

    public List<ContainerDTO> getRunningContainers() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(false) // Only running
                .exec();

        return containers.stream()
                .map(this::mapToDTO)
                .filter(dto -> !dto.getExposedPorts().isEmpty())
                .collect(Collectors.toList());
    }

    private ContainerDTO mapToDTO(Container container) {
        String name = container.getNames().length > 0 ? container.getNames()[0].substring(1) : "Unknown";
        String imageFull = container.getImage();
        List<Integer> ports = Arrays.stream(container.getPorts())
                .map(ContainerPort::getPublicPort)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        String iconUrl = resolveIconUrl(imageFull);

        // Extract Docker Compose project label
        Map<String, String> labels = container.getLabels();
        String project = null;
        if (labels != null) {
            project = labels.get("com.docker.compose.project");
        }

        // Version Detection logic
        String currentVersion = getVersionFromContainer(container);
        String latestVersion = registryService.getLatestVersion(imageFull);
        boolean updateAvailable = checkForUpdate(container, currentVersion, latestVersion);

        return new ContainerDTO(
                container.getId(),
                name,
                imageFull,
                ports,
                iconUrl,
                currentVersion,
                latestVersion,
                updateAvailable,
                project);
    }

    private String getVersionFromContainer(Container container) {
        try {
            // Inspect to get Env variables (ListContainers doesn't provide them reliably)
            var inspect = dockerClient.inspectContainerCmd(container.getId()).exec();

            // 1. Check Environment Variables for *_VERSION (Priority)
            // Strategy: Look for <IMAGE_NAME>_VERSION (e.g. MONGO_VERSION) to avoid
            // GOSU_VERSION etc.
            String[] envs = inspect.getConfig().getEnv();
            if (envs != null) {
                String imageFull = container.getImage();
                // Extract "mongo" from "mongo:jammy" or "library/mongo:latest"
                String imageName = imageFull;
                if (imageName.contains(":"))
                    imageName = imageName.substring(0, imageName.indexOf(":"));
                if (imageName.contains("/"))
                    imageName = imageName.substring(imageName.lastIndexOf("/") + 1);

                String targetEnv = imageName.toUpperCase().replaceAll("[^A-Z0-9]", "_") + "_VERSION";

                for (String env : envs) {
                    if (env.startsWith(targetEnv + "=")) {
                        return env.split("=")[1];
                    }
                    if (env.startsWith("VERSION=")) {
                        return env.split("=")[1];
                    }
                }
            }

            // 2. Check Labels (Standard OCI)
            Map<String, String> labels = inspect.getConfig().getLabels();
            if (labels != null) {
                if (labels.containsKey("org.opencontainers.image.version"))
                    return labels.get("org.opencontainers.image.version");
                if (labels.containsKey("version"))
                    return labels.get("version");
            }
        } catch (Exception e) {
            // Ignore inspect failures
        }

        // 3. Fallback to Tag
        String image = container.getImage();
        if (image.contains(":")) {
            return image.substring(image.lastIndexOf(":") + 1);
        }
        return "latest";
    }

    private boolean checkForUpdate(Container container, String currentVersion, String latestVersion) {
        String imageFull = container.getImage();
        String tag = "latest";
        if (imageFull.contains(":")) {
            tag = imageFull.substring(imageFull.lastIndexOf(":") + 1);
        }

        // 1. Tag-Mismatch Check (e.g. "latest" or "alpine" pointing to new SHA)
        try {
            // Inspect Image to get RepoDigests (Container inspect doesn't have it directly)
            var inspectImage = dockerClient.inspectImageCmd(container.getImageId()).exec();
            List<String> repoDigests = inspectImage.getRepoDigests();

            // Fetch Remote Digest for this specific tag
            String remoteDigest = registryService.getDigest(imageFull, tag);

            if (remoteDigest != null && repoDigests != null) {
                // Check if the remote digest is contained in any of the local repo digests
                // Local format: "image@sha256:hash"
                boolean match = repoDigests.stream().anyMatch(rd -> rd.contains(remoteDigest));
                if (!match) {
                    return true; // Digest mismatch -> Update available!
                }
            }
        } catch (Exception e) {
            // Squelch errors to avoid breaking UI
        }

        // 2. SemVer Check (Upgrade)
        // Only compare if the TAG itself implies a specific version (e.g. "6.0",
        // "7.4.3")
        // If the tag is "alpine", "latest", "jammy", we assume the user wants that
        // specific channel,
        // so we only warn on Digest verification (Step 1).
        if (tag.matches("^v?\\d+(\\.\\d+)*$") && currentVersion != null && latestVersion != null) {
            return !currentVersion.equals(latestVersion);
        }

        return false;
    }

    private String resolveIconUrl(String imageFull) {
        // Extract simple name: "postgres:15" -> "postgres", "my-reg/nginx:latest" ->
        // "nginx"
        String name = imageFull;
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (name.contains(":")) {
            name = name.substring(0, name.indexOf(":"));
        }

        return iconConfigService.resolveIcon(name);
    }
}
