package com.projectestimation.backend.proposal.ai;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;
import org.springframework.stereotype.Service;

@Service
public class GeminiProposalOrchestrator {

    private static final int PROPOSAL_MAX_OUTPUT_TOKENS = 8192;

    private final GeminiProposalPromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final GeminiProposalResponseParser responseParser;

    public GeminiProposalOrchestrator(GeminiProposalPromptBuilder promptBuilder,
                                      GeminiClient geminiClient,
                                      GeminiProposalResponseParser responseParser) {
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.responseParser = responseParser;
    }

    public AiProposalResult generateProposal(Opportunity opportunity,
                                             Parameters parameters,
                                             EstimateResult estimate) {
        String prompt = promptBuilder.build(opportunity, parameters, estimate);
        try {
            String rawResponse = geminiClient.generateContent(prompt, "text/plain", PROPOSAL_MAX_OUTPUT_TOKENS);
            return responseParser.parse(rawResponse);
        } catch (AiGenerationFailedException ex) {
            throw new ProposalFailedException(ex.getMessage(), ex);
        }
    }
}
