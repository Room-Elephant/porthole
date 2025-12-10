package com.roomelephant.porthole.model;

import java.util.List;

public record ContainerDTO(
        String id,
        String name,
        String image,
        List<Integer> exposedPorts,
        String iconUrl,
        String project,
        boolean hasPublicPorts,
        String state,
        String status
) {}
