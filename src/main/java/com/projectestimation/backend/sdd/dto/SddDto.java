package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record SddDto(
        DocumentReleaseHistoryDto documentReleaseHistory,
        CirculationDetailsDto circulationDetails,
        List<AmendmentDto> amendments,

        IntroductionSddDto introduction,

        SystemPerspectiveDto systemPerspective,

        List<ApplicationComponentDto> applicationComponents,

        String classDiagram,

        DatabaseDesignDto databaseDesign,

        SpecialConsiderationsDto specialConsiderations,

        SystemIntegrationStrategyDto systemIntegrationStrategy,

        List<SddTraceabilityMatrixEntryDto> requirementsTraceabilityMatrix
) {}