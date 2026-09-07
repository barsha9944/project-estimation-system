package com.projectestimation.backend.testcase.dto;

import java.util.List;

public record TestCaseDto(

        String reqId,

        String testCaseId,

        String testCondition,

        String testCaseName,

        List<TestScenarioDto> testCaseScenario,

        String testCaseDescription,

        String testData

) {
}