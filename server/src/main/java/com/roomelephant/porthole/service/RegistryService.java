package com.roomelephant.porthole.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class RegistryService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private static final Pattern SEMVER_PATTERN = Pattern.compile("^v?\\d+(\\.\\d+)+$");

    public String getDigest(String imageName, String tag) {
        try {
            String repository = resolveRepository(imageName);
            String token = fetchAuthToken(repository);

            if (token == null)
                return null;

            String url = "https://registry-1.docker.io/v2/" + repository + "/manifests/" + tag;

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(token);
            headers.set("Accept", "application/vnd.docker.distribution.manifest.v2+json");

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            var response = restTemplate.exchange(url, org.springframework.http.HttpMethod.HEAD, entity, Void.class);
            return response.getHeaders().getFirst("Docker-Content-Digest");

        } catch (Exception e) {
            System.err.println("Failed to fetch digest for " + imageName + ":" + tag + " - " + e.getMessage());
            return null;
        }
    }

    private String fetchAuthToken(String repository) {
        try {
            String url = "https://auth.docker.io/token?service=registry.docker.io&scope=repository:" + repository
                    + ":pull";
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            return response != null && response.has("token") ? response.get("token").asText() : null;
        } catch (Exception e) {
            System.err.println("Failed to fetch auth token for " + repository);
            return null;
        }
    }

    private String resolveRepository(String imageName) {
        String repository = imageName.contains("/") ? imageName : "library/" + imageName;
        if (repository.contains(":")) {
            repository = repository.substring(0, repository.indexOf(":"));
        }
        return repository;
    }

    public String getLatestVersion(String imageName) {
        if (cache.containsKey(imageName)) {
            return cache.get(imageName);
        }

        try {
            // Handle official library images (e.g., "mongo" -> "library/mongo")
            String repository = resolveRepository(imageName);
            // Remove tag if present for the API call
            if (repository.contains(":")) {
                repository = repository.substring(0, repository.indexOf(":"));
            }

            String url = "https://hub.docker.com/v2/repositories/" + repository + "/tags?page_size=100";
            String latest = fetchLatestFromHub(url);

            if (latest != null) {
                cache.put(imageName, latest);
            }
            return latest;

        } catch (Exception e) {
            System.err.println("Failed to fetch tags for " + imageName + ": " + e.getMessage());
            return null;
        }
    }

    private String fetchLatestFromHub(String url) {
        try {
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response == null || !response.has("results"))
                return null;

            List<String> tags = new ArrayList<>();
            for (JsonNode result : response.get("results")) {
                String name = result.get("name").asText();
                if (SEMVER_PATTERN.matcher(name).matches()) {
                    tags.add(name);
                }
            }

            // Simple semantic sort
            tags.sort(this::compareSemVer);
            if (tags.isEmpty())
                return null;

            return tags.get(tags.size() - 1); // Return the highest version
        } catch (Exception e) {
            return null;
        }
    }

    private int compareSemVer(String v1, String v2) {
        String[] parts1 = v1.replaceAll("^v", "").split("\\.");
        String[] parts2 = v2.replaceAll("^v", "").split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }
}
