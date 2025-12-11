package com.roomelephant.porthole.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomelephant.porthole.config.properties.DashboardProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class IconConfig {

    @Bean
    public Map<String, String> iconMappings(DashboardProperties dashboardProperties) {
        Map<String, String> mappings = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();

        // Load internal defaults
        try {
            ClassPathResource resource = new ClassPathResource("icons.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, String> defaults = objectMapper.readValue(inputStream, new TypeReference<>() {});
                    mappings.putAll(defaults);
                }
            }
        } catch (IOException _) {
            // Should not happen
        }

        // Load external overrides
        File externalFile = new File(dashboardProperties.icons().path());
        if (externalFile.exists()) {
            try {
                Map<String, String> external = objectMapper.readValue(externalFile, new TypeReference<>() {});
                mappings.putAll(external);
                log.debug("Loaded external icon config from {}", dashboardProperties.icons().path());
            } catch (IOException e) {
                log.error("Failed to load external icon config: {}", e.getMessage());
            }
        }

        log.debug("Icon config initialized with {} mappings", mappings.size());
        return Collections.unmodifiableMap(mappings);
    }
}

