package com.projectestimation.backend.testcase.dto;

public record TestStepDto(

        Integer stepNumber,

        String stepDescription,

        String expectedResult,

        String actualResult,

        String testStatus,

        String passFail,

        String defectId,

        String severity,

        String defectType,

        String rootCause,

        String phaseIntroduced

) {
}