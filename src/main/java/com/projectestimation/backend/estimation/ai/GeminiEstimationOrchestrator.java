package com.projectestimation.backend.estimation.ai;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;
import org.springframework.stereotype.Service;

@Service
public class GeminiEstimationOrchestrator {

    private final GeminiEstimationPromptBuilder promptBuilder;
    private final GeminiEstimationClient geminiClient;
    private final GeminiEstimationResponseParser responseParser;

    public GeminiEstimationOrchestrator(GeminiEstimationPromptBuilder promptBuilder,
                                        GeminiEstimationClient geminiClient,
                                        GeminiEstimationResponseParser responseParser) {
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.responseParser = responseParser;
    }

    public AiEstimationResult estimate(Opportunity opportunity, Parameters parameters) {
        String prompt = promptBuilder.build(opportunity, parameters);
        String rawResponse = geminiClient.generateEstimation(prompt);
        return responseParser.parse(rawResponse);
    }

    public AiEstimationResult estimateFromLegacyPayload(
            String projectName,
            String requirementSummary,
            double complexityFactor,
            double riskFactor,
            double productivityFactor,
            double hourlyRate,
            int teamSize
    ) {
        String prompt = promptBuilder.buildFromLegacyPayload(
                projectName,
                requirementSummary,
                complexityFactor,
                riskFactor,
                productivityFactor,
                hourlyRate,
                teamSize
        );
        String rawResponse = geminiClient.generateEstimation(prompt);
        return responseParser.parse(rawResponse);
    }
}
