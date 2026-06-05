package com.projectestimation.backend.estimation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.EstimationFailedException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GeminiEstimationResponseParser {

    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*}", Pattern.DOTALL);

    private final ObjectMapper objectMapper;

    public GeminiEstimationResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiEstimationResult parse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new EstimationFailedException("Gemini returned an empty estimation response");
        }

        try {
            String jsonPayload = extractJson(rawResponse);
            JsonNode root = objectMapper.readTree(jsonPayload);

            double totalEffortHours = requirePositiveNumber(root, "totalEffortHours");
            double estimatedCost = requireNonNegativeNumber(root, "estimatedCost");
            double timelineWeeks = requirePositiveNumber(root, "timelineWeeks");
            double confidenceScore = requireConfidence(root, "confidenceScore");
            String breakdown = requireText(root, "breakdown");
            String reasoning = requireText(root, "reasoning");

            return new AiEstimationResult(
                    totalEffortHours,
                    estimatedCost,
                    timelineWeeks,
                    confidenceScore,
                    breakdown,
                    reasoning
            );
        } catch (EstimationFailedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EstimationFailedException("Failed to parse Gemini estimation response", ex);
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

        throw new EstimationFailedException("Gemini response did not contain valid JSON");
    }

    private double requirePositiveNumber(JsonNode root, String field) {
        double value = requireNumber(root, field);
        if (value <= 0) {
            throw new EstimationFailedException("Gemini response field '" + field + "' must be greater than 0");
        }
        return value;
    }

    private double requireNonNegativeNumber(JsonNode root, String field) {
        double value = requireNumber(root, field);
        if (value < 0) {
            throw new EstimationFailedException("Gemini response field '" + field + "' must be non-negative");
        }
        return value;
    }

    private double requireConfidence(JsonNode root, String field) {
        double value = requireNumber(root, field);
        if (value < 0 || value > 100) {
            throw new EstimationFailedException("Gemini response field '" + field + "' must be between 0 and 100");
        }
        return value;
    }

    private double requireNumber(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isNumber()) {
            throw new EstimationFailedException("Gemini response is missing required numeric field: " + field);
        }
        return node.asDouble();
    }

    private String requireText(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.asText().isBlank()) {
            throw new EstimationFailedException("Gemini response is missing required text field: " + field);
        }
        return node.asText().trim();
    }
}
