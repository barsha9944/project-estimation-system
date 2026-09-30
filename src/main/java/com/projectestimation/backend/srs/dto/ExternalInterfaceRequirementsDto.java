package com.projectestimation.backend.srs.dto;

import java.util.List;

public record ExternalInterfaceRequirementsDto(
        List<InterfaceRequirementDto> softwareInterfaces,
        List<String> hardwareInterfaces,
        List<String> communicationInterfaces,
        String userInterfaces
) {
}
