package com.roomelephant.porthole.model;

public record VersionDTO(String currentVersion, String latestVersion, boolean updateAvailable) {}
