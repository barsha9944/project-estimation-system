package com.projectestimation.backend.testcase.dto;

import java.util.List;

public record TestCaseGenerationResponse(
        List<TestCaseDto> testCases
) {
}