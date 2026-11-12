
package com.projectestimation.backend.car.ai;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.common.ai.gateway.AiGateway;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeminiCarOrchestrator {

    private final AiGateway aiGateway;
    private final CarPromptBuilder promptBuilder;

    public String generateRootCauseAnalysis(
            CarAnalysis car,
            ProjectMetricsResponse projectMetrics) {

        String prompt = promptBuilder.buildRootCausePrompt(
                car,
                projectMetrics);

        return aiGateway.generateJsonContent(prompt, 8192);
    }
}
