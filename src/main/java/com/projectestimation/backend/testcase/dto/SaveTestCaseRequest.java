package com.projectestimation.backend.testcase.dto;

import java.util.List;

public record SaveTestCaseRequest(
        List<TestCaseDto> testCases
) {
}