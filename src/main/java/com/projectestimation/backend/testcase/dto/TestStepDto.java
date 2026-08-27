package com.projectestimation.backend.testcase.dto;

public record TestStepDto(
        Integer stepNumber,
        String stepDescription,
        String expectedResult
) {
}