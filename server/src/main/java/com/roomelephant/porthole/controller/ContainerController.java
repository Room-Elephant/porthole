package com.roomelephant.porthole.controller;

import com.roomelephant.porthole.model.ContainerDTO;
import com.roomelephant.porthole.service.DockerService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow all for simplicity in dev
public class ContainerController {

    private final DockerService dockerService;

    public ContainerController(DockerService dockerService) {
        this.dockerService = dockerService;
    }

    @GetMapping("/containers")
    public List<ContainerDTO> getContainers() {
        return dockerService.getRunningContainers();
    }
}
