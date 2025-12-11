package com.roomelephant.porthole.controller;

import com.roomelephant.porthole.model.ContainerDTO;
import com.roomelephant.porthole.model.VersionDTO;
import com.roomelephant.porthole.service.ContainerService;
import com.roomelephant.porthole.service.VersionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContainerController.class)
@DisplayName("ContainerController")
class ContainerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContainerService containerService;

    @MockitoBean
    private VersionService versionService;

    @Nested
    @DisplayName("GET /api/containers")
    class GetContainers {

        @Test
        @DisplayName("should return containers with default parameters")
        void shouldReturnContainersWithDefaultParameters() throws Exception {
            List<ContainerDTO> containers = List.of(
                    createContainerDTO("container1"),
                    createContainerDTO("container2")
            );
            when(containerService.getContainers(false, false)).thenReturn(containers);

            mockMvc.perform(get("/api/containers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("container1"))
                    .andExpect(jsonPath("$[1].name").value("container2"));

            verify(containerService).getContainers(false, false);
        }

        @Test
        @DisplayName("should pass includeWithoutPorts parameter")
        void shouldPassIncludeWithoutPortsParameter() throws Exception {
            when(containerService.getContainers(true, false)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/containers")
                            .param("includeWithoutPorts", "true"))
                    .andExpect(status().isOk());

            verify(containerService).getContainers(true, false);
        }

        @Test
        @DisplayName("should pass includeStopped parameter")
        void shouldPassIncludeStoppedParameter() throws Exception {
            when(containerService.getContainers(false, true)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/containers")
                            .param("includeStopped", "true"))
                    .andExpect(status().isOk());

            verify(containerService).getContainers(false, true);
        }

        @Test
        @DisplayName("should pass both parameters")
        void shouldPassBothParameters() throws Exception {
            when(containerService.getContainers(true, true)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/containers")
                            .param("includeWithoutPorts", "true")
                            .param("includeStopped", "true"))
                    .andExpect(status().isOk());

            verify(containerService).getContainers(true, true);
        }

        @Test
        @DisplayName("should return empty array when no containers")
        void shouldReturnEmptyArrayWhenNoContainers() throws Exception {
            when(containerService.getContainers(false, false)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/containers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/containers/{containerId}/version")
    class GetVersion {

        @Test
        @DisplayName("should return version info")
        void shouldReturnVersionInfo() throws Exception {
            VersionDTO version = new VersionDTO("1.0.0", "1.1.0", true);
            when(versionService.getVersionInfo("container-123")).thenReturn(version);

            mockMvc.perform(get("/api/containers/container-123/version"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentVersion").value("1.0.0"))
                    .andExpect(jsonPath("$.latestVersion").value("1.1.0"))
                    .andExpect(jsonPath("$.updateAvailable").value(true));

            verify(versionService).getVersionInfo("container-123");
        }

        @Test
        @DisplayName("should handle container with no update available")
        void shouldHandleContainerWithNoUpdateAvailable() throws Exception {
            VersionDTO version = new VersionDTO("2.0.0", "2.0.0", false);
            when(versionService.getVersionInfo("container-456")).thenReturn(version);

            mockMvc.perform(get("/api/containers/container-456/version"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.updateAvailable").value(false));
        }
    }

    private ContainerDTO createContainerDTO(String name) {
        return new ContainerDTO(
                name + "-id",
                name,
                name,
                "nginx:latest",
                Set.of(80),
                "https://example.com/nginx.png",
                "test-project",
                "running",
                "Up 1 hour"
        );
    }
}
