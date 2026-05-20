package com.projectestimation.backend.estimation.ai;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.common.exception.EstimationFailedException;
import org.springframework.stereotype.Component;

@Component
public class GeminiEstimationClient {

    private final GeminiClient geminiClient;

    public GeminiEstimationClient(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    public String generateEstimation(String prompt) {
        try {
            return geminiClient.generateJsonContent(prompt);
        } catch (AiGenerationFailedException ex) {
            throw new EstimationFailedException(ex.getMessage(), ex);
        }
    }
}
