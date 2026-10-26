
package com.projectestimation.backend.pmp.review.service;

import org.springframework.stereotype.Service;
import com.projectestimation.backend.pmp.review.ai.GeminiPmpReviewOrchestrator;

@Service
public class PmpReviewNoteGenerator {

    private final GeminiPmpReviewOrchestrator gemini;

    public PmpReviewNoteGenerator(
            GeminiPmpReviewOrchestrator gemini) {
        this.gemini = gemini;
    }

    public String generate(Long opportunityId, String pmpContent) {
        if (opportunityId == null || opportunityId <= 0) {
            throw new IllegalArgumentException(
                "Opportunity ID must be a positive number."
            );
        }

        if (pmpContent == null || pmpContent.isBlank()) {
            throw new IllegalArgumentException(
                "PMP content cannot be empty."
            );
        }

        return gemini.generate(
            "REVIEW_NOTE",
            opportunityId,
            pmpContent
        );
    }
}
