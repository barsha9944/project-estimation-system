package com.projectestimation.backend.pmp.review.ai;

import com.projectestimation.backend.common.ai.GeminiClient;
import org.springframework.stereotype.Component;

@Component
public class GeminiPmpReviewOrchestrator {

    private final GeminiClient geminiClient;
    private final PmpReviewPromptBuilder promptBuilder;

    public GeminiPmpReviewOrchestrator(
            GeminiClient geminiClient,
            PmpReviewPromptBuilder promptBuilder) {

        this.geminiClient = geminiClient;
        this.promptBuilder = promptBuilder;
    }

    public String generate(
            String documentType,
            Long opportunityId,
            String pmpContent) {

        if (pmpContent == null || pmpContent.isBlank()) {
            throw new IllegalArgumentException(
                "PMP content cannot be empty."
            );
        }

        String prompt = promptBuilder.buildPrompt(
            documentType,
            opportunityId,
            pmpContent
        );

        try {
            return geminiClient.generateJsonContent(prompt, 8192);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "Gemini failed to generate " + documentType,
                exception
            );
        }
    }
}