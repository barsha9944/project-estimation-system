package com.projectestimation.backend.sdd.dto;

public record SpecialConsiderationsDto(
        String designRulesCriteriaToFollow,
        String programmingRulesToFollow,
        String errorHandlingProcedures,
        String specialSecurityProvisions,
        String specialRecoveryProcedure,
        String auditTracingFacility,
        String systemImplementationProcedures,
        String databaseAdministrationProcedure
) {}