package com.roomelephant.porthole.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class IconConfigService {

    @Value("${dashboard.icons.path}")
    private String externalConfigPath;

    @Value("${dashboard.icons.url}")
    private String iconsBaseUrl;

    @Value("${dashboard.icons.extension}")
    private String iconsExtension;

    private final Map<String, String> customMappings = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadConfigurations() {
        try {
            ClassPathResource resource = new ClassPathResource("icons.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, String> defaults = objectMapper.readValue(inputStream, new TypeReference<>() {
                    });
                    customMappings.putAll(defaults);
                }
            }
        } catch (IOException _) {
            // Should not happen
        }

        File externalFile = new File(externalConfigPath);
        if (externalFile.exists()) {
            try {
                Map<String, String> external = objectMapper.readValue(externalFile, new TypeReference<>() {
                });
                customMappings.putAll(external);
                log.debug("Loaded external icon config from {}", externalConfigPath);
            } catch (IOException e) {
                log.error("Failed to load external icon config: {}", e.getMessage());
            }
        }

        log.debug("Icon service initialized with {} custom mappings", customMappings.size());
    }

    public @NonNull String resolveIcon(@NonNull String imageName) {
        if (customMappings.containsKey(imageName)) {
            return buildDashboardIconsUrl(customMappings.get(imageName));
        }

        String iconName = sanitizeIconName(imageName);
        return buildDashboardIconsUrl(iconName);
    }

    private @NonNull String sanitizeIconName(@NonNull String name) {
        return name.toLowerCase().replace('_', '-');
    }

    private @NonNull String buildDashboardIconsUrl(String iconName) {
        return iconsBaseUrl + "/" + iconName + iconsExtension;
    }
}
