package com.roomelephant.porthole.model;

import java.util.List;

public class ContainerDTO {
    private String id;
    private String name;
    private String image;
    private List<Integer> exposedPorts;
    private String iconUrl;
    private String currentVersion;
    private String latestVersion;
    private boolean updateAvailable;
    private String project; // Docker Compose project name
    private boolean hasPublicPorts; // Whether container has publicly mapped ports

    public ContainerDTO() {
    }

    public ContainerDTO(String id, String name, String image, List<Integer> exposedPorts, String iconUrl,
            String currentVersion, String latestVersion, boolean updateAvailable, String project,
            boolean hasPublicPorts) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.exposedPorts = exposedPorts;
        this.iconUrl = iconUrl;
        this.currentVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.updateAvailable = updateAvailable;
        this.project = project;
        this.hasPublicPorts = hasPublicPorts;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<Integer> getExposedPorts() {
        return exposedPorts;
    }

    public void setExposedPorts(List<Integer> exposedPorts) {
        this.exposedPorts = exposedPorts;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public boolean isHasPublicPorts() {
        return hasPublicPorts;
    }

    public void setHasPublicPorts(boolean hasPublicPorts) {
        this.hasPublicPorts = hasPublicPorts;
    }
}
