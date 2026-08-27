package com.projectestimation.backend.testcase.dto;

import java.util.List;

public record TestCaseDto(
        String reqId,
        String testCaseId,
        String testCaseName,
        String testCaseDescription,
        String testData,
        List<TestStepDto> steps
) {
}