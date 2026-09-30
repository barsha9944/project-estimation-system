package com.projectestimation.backend.srs.dto;

public record NonFunctionalRequirementsDto(
        String accuracy,
        String auditTrail,
        String availability,
        String capacityLimits,
        String dataRetention,
        String performance,
        String portability,
        String recoverability,
        String reliability,
        String securityRequirements,
        String otherComplianceRequirements
) {
}
