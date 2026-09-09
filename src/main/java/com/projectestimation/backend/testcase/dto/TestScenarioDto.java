package com.projectestimation.backend.testcase.dto;

import java.util.List;

public record TestScenarioDto(

        String scenarioId,

        String scenarioName,

        String scenarioType,

        String testCondition,
        
        List<TestStepDto> steps

) {
}