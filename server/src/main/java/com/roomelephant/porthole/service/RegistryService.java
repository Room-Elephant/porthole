package com.roomelephant.porthole.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.roomelephant.porthole.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class RegistryService {

    private static final String REGISTRY_URL = "https://registry-1.docker.io/v2/";
    private static final String REGISTRY_URL_MANIFESTS = "/manifests/";
    private static final String AUTH_URL = "https://auth.docker.io/token?service=registry.docker.io&scope=repository:";
    private static final String AUTH_URL_PULL = ":pull";
    private static final String REPOSITORIES_URL = "https://hub.docker.com/v2/repositories/";
    private static final String REPOSITORIES_URL_TAGS = "/tags?page_size=100";
    private static final String BEARER = "Bearer ";
    private static final String TOKEN = "token";
    private static final String ACCEPT_HEADER = "application/vnd.docker.distribution.manifest.v2+json";
    private static final String DOCKER_CONTENT_DIGEST = "Docker-Content-Digest";
    private static final String RESULTS = "results";
    private static final String NAME = "name";

    private final RestClient restClient;
    private final Cache<String, String> versionCache;

    public RegistryService(RestClient restClient, @Value("${registry.cache.ttl}") Duration cacheTtl) {
        this.restClient = restClient;
        this.versionCache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtl)
                .maximumSize(100)
                .build();
    }

    public @Nullable String getDigest(@NonNull String imageName, String tag) {
        try {
            String repository = ImageUtils.resolveRepository(imageName);
            String token = fetchAuthToken(repository);

            if (token == null)
                return null;

            return fetchDigest(tag, repository, token);
        } catch (Exception e) {
            log.warn("Failed to fetch digest for {}:{} - {}", imageName, tag, e.getMessage());
            return null;
        }
    }

    public @Nullable String getLatestVersion(@NonNull String imageName) {
        try {
            return versionCache.get(imageName, this::fetchLatestVersion);
        } catch (Exception e) {
            log.warn("Failed to fetch tags for {}: {}", imageName, e.getMessage());
            return null;
        }
    }

    private @Nullable String fetchLatestVersion(String imageName) {
        String repository = ImageUtils.resolveRepository(imageName);
        return fetchLatestFromHub(repository);
    }

    private @Nullable String fetchDigest(String tag, String repository, String token) {
        String url = REGISTRY_URL + repository + REGISTRY_URL_MANIFESTS + tag;

        var response = restClient.head()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, BEARER + token)
                .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                .retrieve()
                .toBodilessEntity();

        return response.getHeaders().getFirst(DOCKER_CONTENT_DIGEST);
    }

    private @Nullable String fetchAuthToken(String repository) {
        try {
            String url = AUTH_URL + repository + AUTH_URL_PULL;
            JsonNode response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.has(TOKEN) ? response.get(TOKEN).asText() : null;
        } catch (Exception e) {
            log.warn("Failed to fetch auth token for {}: {}", repository, e.getMessage());
            return null;
        }
    }

    private @Nullable String fetchLatestFromHub(String repository) {
        String url = REPOSITORIES_URL + repository + REPOSITORIES_URL_TAGS;
        try {
            JsonNode response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has(RESULTS))
                return null;

            List<String> tags = new ArrayList<>();
            for (JsonNode result : response.get(RESULTS)) {
                String name = result.get(NAME).asText();
                if (ImageUtils.isSemver(name)) {
                    tags.add(name);
                }
            }

            tags.sort(ImageUtils::compareSemVer);
            if (tags.isEmpty())
                return null;

            return tags.getLast();
        } catch (Exception _) {
            return null;
        }
    }
}
