package com.projectestimation.backend.proposal.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiProposalResponseParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public GeminiProposalResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiProposalResult parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new ProposalFailedException("Gemini returned an empty proposal response");
        }

        try {
            String jsonPayload = extractJson(rawResponse);
            JsonNode root = objectMapper.readTree(jsonPayload);

            String executiveSummary = requireText(root, "executiveSummary");
            String projectOverview = requireText(root, "projectOverview");
            String scopeOfWork = requireText(root, "scopeOfWork");
            String technologyStack = requireText(root, "technologyStack");
            String componentsAndFeatures = requireText(root, "componentsAndFeatures");
            String deliveryApproach = requireText(root, "deliveryApproach");
            String teamStructure = requireText(root, "teamStructure");
            String timelineEstimate = requireText(root, "timelineEstimate");
            String costEstimate = requireText(root, "costEstimate");
            String risksAndAssumptions = requireText(root, "risksAndAssumptions");
            String supportAndMaintenance = requireText(root, "supportAndMaintenance");
            String conclusion = requireText(root, "conclusion");

            String proposalContent = buildProposalContent(
                    executiveSummary,
                    projectOverview,
                    scopeOfWork,
                    technologyStack,
                    componentsAndFeatures,
                    deliveryApproach,
                    teamStructure,
                    timelineEstimate,
                    costEstimate,
                    risksAndAssumptions,
                    supportAndMaintenance,
                    conclusion
            );

            return new AiProposalResult(
                    executiveSummary,
                    projectOverview,
                    scopeOfWork,
                    technologyStack,
                    componentsAndFeatures,
                    deliveryApproach,
                    teamStructure,
                    timelineEstimate,
                    costEstimate,
                    risksAndAssumptions,
                    supportAndMaintenance,
                    conclusion,
                    proposalContent
            );
        } catch (ProposalFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProposalFailedException("Failed to parse Gemini proposal response", ex);
        }
    }

    private String extractJson(String rawResponse) {
        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("{")) {
            return trimmed;
        }

        Matcher matcher = JSON_BLOCK.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group();
        }

        throw new ProposalFailedException("Gemini response did not contain valid JSON");
    }

    private String requireText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.asText().isBlank()) {
            throw new ProposalFailedException("Gemini response is missing required field: " + field);
        }
        return node.asText().trim();
    }

    private String buildProposalContent(String executiveSummary,
                                        String projectOverview,
                                        String scopeOfWork,
                                        String technologyStack,
                                        String componentsAndFeatures,
                                        String deliveryApproach,
                                        String teamStructure,
                                        String timelineEstimate,
                                        String costEstimate,
                                        String risksAndAssumptions,
                                        String supportAndMaintenance,
                                        String conclusion) {
        return """
                EXECUTIVE SUMMARY
                %s

                PROJECT OVERVIEW
                %s

                SCOPE OF WORK
                %s

                TECHNOLOGY STACK
                %s

                COMPONENTS & FEATURES
                %s

                DELIVERY APPROACH
                %s

                TEAM STRUCTURE
                %s

                TIMELINE ESTIMATE
                %s

                COST ESTIMATE
                %s

                RISKS & ASSUMPTIONS
                %s

                SUPPORT & MAINTENANCE
                %s

                CONCLUSION
                %s
                """.formatted(
                executiveSummary,
                projectOverview,
                scopeOfWork,
                technologyStack,
                componentsAndFeatures,
                deliveryApproach,
                teamStructure,
                timelineEstimate,
                costEstimate,
                risksAndAssumptions,
                supportAndMaintenance,
                conclusion
        );
    }
}
