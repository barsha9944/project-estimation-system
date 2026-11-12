
package com.projectestimation.backend.pmp.review.service;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.pmp.review.ai.GeminiPmpReviewOrchestrator;

@Service
public class PmpReviewFollowUpGenerator {

    private final GeminiPmpReviewOrchestrator gemini;

    public PmpReviewFollowUpGenerator(
            GeminiPmpReviewOrchestrator gemini) {
        this.gemini = gemini;
    }

    public String generate(
            Long opportunityId,
            String pmpContent,
            String reviewNote,
            String defectTracking) {

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

        if (reviewNote == null || reviewNote.isBlank()) {
            throw new IllegalArgumentException(
                "PMP Review Note must be generated before the follow-up."
            );
        }

        if (defectTracking == null || defectTracking.isBlank()) {
            throw new IllegalArgumentException(
                "Defect Tracking Sheet must be generated before the follow-up."
            );
        }

        String promptContext = """
            ORIGINAL PROJECT MANAGEMENT PLAN:
            %s

            PMP REVIEW NOTE:
            %s

            DEFECT TRACKING SHEET:
            %s

            Generate a separate PMP Review Follow-up Note.

            Requirements:
            1. Refer to the original PMP and both initial review documents.
            2. Distinguish resolved, unresolved, and unverified defects.
            3. Do not assume a corrective action was completed without evidence.
            4. Do not mark a defect as closed without verified evidence.
            5. Do not invent reviewer comments, signatures, or approvals.
            6. Use "Not provided" for missing information.
            7. Read the latest uploaded defect tracking sheet as the
			   authoritative record of defect status.
			
			8. Compare every defect against its previous status.
			
			9. Clearly identify defects that remain OPEN, IN PROGRESS,
			   RESOLVED, or CLOSED.
			
			10. Do not treat RESOLVED as CLOSED.
			
			11. Do not declare the review completed if any defect is
			    still open, in progress, resolved but unverified,
			    or missing closure evidence.
			
			12. Identify the corrective action and closure evidence
			    for each defect.
            """.formatted(
                pmpContent,
                reviewNote,
                defectTracking
            );

        return gemini.generate(
            "FOLLOW_UP",
            opportunityId,
            promptContext
        );
    }
}
