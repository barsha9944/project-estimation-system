package com.projectestimation.backend.srs.dto;

import java.util.List;

public record SrsDto(

        DocumentInformationDto documentInformation,

        IntroductionDto introduction,

        OverallProductRequirementsDto overallProductRequirements,

        ExternalInterfaceRequirementsDto externalInterfaceRequirements,

        ProposedSystemEnvironmentsDto proposedSystemEnvironments,

        List<FunctionalRequirementDto> functionalRequirements,

        NonFunctionalRequirementsDto nonFunctionalRequirements,

        DataRequirementsDto dataRequirements,

        List<TraceabilityMatrixEntryDto> requirementsTraceabilityMatrix

) {
}
