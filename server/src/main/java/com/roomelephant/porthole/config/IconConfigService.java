package com.roomelephant.porthole.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class IconConfigService {

    @Value("${dashboard.icons.path:/app/config/icons.json}")
    private String externalConfigPath;

    private Map<String, String> customMappings = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        System.out.println("Icon service initialized with " + customMappings.size() + " custom mappings");
    }

    public String resolveIcon(String imageName) {
        // 1. Check custom mappings first (highest priority)
        if (customMappings.containsKey(imageName)) {
            return buildDashboardIconsUrl(customMappings.get(imageName));
        }

        // 2. Sanitize and use image name directly
        String iconName = sanitizeIconName(imageName);
        return buildDashboardIconsUrl(iconName);
    }

    private String sanitizeIconName(String name) {
        // Convert to lowercase kebab-case
        return name.toLowerCase().replaceAll("_", "-");
    }

    private String buildDashboardIconsUrl(String iconName) {
        return "https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/webp/" + iconName + ".webp";
    }
}
