package com.roomelephant.porthole.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomelephant.porthole.model.SimpleIcon;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class IconConfigService {

    @Value("${dashboard.icons.path:/app/config/icons.json}")
    private String externalConfigPath;

    @Value("${dashboard.icons.api.url:https://unpkg.com/simple-icons@latest/icons.json}")
    private String simpleIconsApiUrl;

    private Map<String, String> customMappings = new HashMap<>();
    private List<SimpleIcon> simpleIconsIndex = null;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void loadConfigurations() {
        // 1. Load custom mappings from classpath
        try {
            ClassPathResource resource = new ClassPathResource("icons.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, String> defaults = objectMapper.readValue(inputStream,
                            new TypeReference<Map<String, String>>() {
                            });
                    customMappings.putAll(defaults);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load default icons.json: " + e.getMessage());
        }

        // 2. Load external custom mappings (overrides defaults)
        File externalFile = new File(externalConfigPath);
        if (externalFile.exists()) {
            try {
                Map<String, String> external = objectMapper.readValue(externalFile,
                        new TypeReference<Map<String, String>>() {
                        });
                customMappings.putAll(external);
                System.out.println("Loaded external icon config from " + externalConfigPath);
            } catch (IOException e) {
                System.err.println("Failed to load external icon config: " + e.getMessage());
            }
        } else {
            System.out.println("No external icon config found at " + externalConfigPath);
        }

        // 3. Fetch SimpleIcons index
        fetchSimpleIconsIndex();
    }

    private void fetchSimpleIconsIndex() {
        try {
            System.out.println("Fetching SimpleIcons index from " + simpleIconsApiUrl);
            String response = restTemplate.getForObject(simpleIconsApiUrl, String.class);
            simpleIconsIndex = objectMapper.readValue(response, new TypeReference<List<SimpleIcon>>() {
            });
            System.out.println("Loaded " + simpleIconsIndex.size() + " icons from SimpleIcons API");
        } catch (Exception e) {
            System.err.println("Failed to fetch SimpleIcons index: " + e.getMessage());
            System.err.println("Icon matching will fall back to direct CDN URL construction");
        }
    }

    public String resolveIcon(String imageName) {
        // 1. Check custom mappings first (highest priority)
        if (customMappings.containsKey(imageName)) {
            return "https://cdn.simpleicons.org/" + customMappings.get(imageName);
        }

        // 2. Try fuzzy matching with SimpleIcons index
        String slug = fuzzyMatch(imageName);

        return "https://cdn.simpleicons.org/" + slug;
    }

    private String fuzzyMatch(String imageName) {
        // If SimpleIcons index failed to load, fall back to direct slug
        if (simpleIconsIndex == null) {
            return imageName;
        }

        String normalizedInput = imageName.toLowerCase();

        // Strategy 1: Exact slug match (case-insensitive)
        for (SimpleIcon icon : simpleIconsIndex) {
            if (icon.getSlug() != null && icon.getSlug().equalsIgnoreCase(imageName)) {
                return icon.getSlug();
            }
        }

        // Strategy 2: Exact title match (case-insensitive)
        for (SimpleIcon icon : simpleIconsIndex) {
            if (icon.getTitle() != null && icon.getTitle().equalsIgnoreCase(imageName)) {
                return icon.getSlug();
            }
        }

        // Strategy 3: Slug starts with input (e.g., "traefik" -> "traefikproxy")
        // Only if input is at least 4 characters to avoid false matches
        if (normalizedInput.length() >= 4) {
            for (SimpleIcon icon : simpleIconsIndex) {
                if (icon.getSlug() != null
                        && icon.getSlug().toLowerCase().startsWith(normalizedInput)) {
                    return icon.getSlug();
                }
            }
        }

        // Strategy 4: Title starts with input (e.g., "next" -> "Nextcloud")
        // Only if input is at least 4 characters to avoid false matches
        if (normalizedInput.length() >= 4) {
            for (SimpleIcon icon : simpleIconsIndex) {
                if (icon.getTitle() != null
                        && icon.getTitle().toLowerCase().startsWith(normalizedInput)) {
                    return icon.getSlug();
                }
            }
        }

        // Strategy 5: Input starts with slug (reverse match, e.g., "traefikproxy"
        // contains "traefik")
        // Only match if slug is at least 4 characters to avoid single-letter matches
        for (SimpleIcon icon : simpleIconsIndex) {
            if (icon.getSlug() != null
                    && icon.getSlug().length() >= 4
                    && normalizedInput.startsWith(icon.getSlug().toLowerCase())) {
                return icon.getSlug();
            }
        }

        // Fallback: Return original input as slug
        return imageName;
    }
}
