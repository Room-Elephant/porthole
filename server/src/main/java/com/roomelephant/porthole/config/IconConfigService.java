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

    private Map<String, String> iconMappings = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadConfigurations() {
        // 1. Load defaults from classpath
        try {
            ClassPathResource resource = new ClassPathResource("icons.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, String> defaults = objectMapper.readValue(inputStream,
                            new TypeReference<Map<String, String>>() {
                            });
                    iconMappings.putAll(defaults);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load default icons.json: " + e.getMessage());
        }

        // 2. Load external config (overrides defaults)
        File externalFile = new File(externalConfigPath);
        if (externalFile.exists()) {
            try {
                Map<String, String> external = objectMapper.readValue(externalFile,
                        new TypeReference<Map<String, String>>() {
                        });
                iconMappings.putAll(external);
                System.out.println("Loaded external icon config from " + externalConfigPath);
            } catch (IOException e) {
                System.err.println("Failed to load external icon config: " + e.getMessage());
            }
        } else {
            System.out.println("No external icon config found at " + externalConfigPath);
        }
    }

    public String resolveIcon(String imageName) {
        String slug = iconMappings.getOrDefault(imageName, imageName);
        return "https://cdn.simpleicons.org/" + slug;
    }
}
