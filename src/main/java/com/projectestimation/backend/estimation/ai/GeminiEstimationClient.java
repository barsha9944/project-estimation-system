package com.projectestimation.backend.estimation.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectestimation.backend.common.exception.EstimationFailedException;
import com.projectestimation.backend.estimation.config.GeminiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GeminiEstimationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeminiProperties properties;

    public GeminiEstimationClient(
            @Qualifier("geminiRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            GeminiProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String generateEstimation(String prompt) {
        validateConfiguration();

        try {
            String url = properties.getBaseUrl() + "/models/" + properties.getModel() + ":generateContent";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", properties.getApiKey());

            HttpEntity<String> request = new HttpEntity<>(buildRequestBody(prompt), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new EstimationFailedException("Gemini API returned an unsuccessful response");
            }

            return extractResponseText(response.getBody());
        } catch (EstimationFailedException ex) {
            throw ex;
        } catch (RestClientException ex) {
        	if (ex.getMessage() != null &&
        	        ex.getMessage().contains("429")) {

        	        return getMockEstimationResponse();
        	    }

        	    throw new EstimationFailedException(
        	        "Gemini API request failed or timed out",
        	        ex
        	    );
        } catch (Exception ex) {
            throw new EstimationFailedException("Unexpected error while calling Gemini API", ex);
        }
    }

    private String getMockEstimationResponse() {

        return """
        {
          "totalEffortHours": 320,
          "estimatedCost": 16000,
          "timelineWeeks": 8,
          "confidenceScore": 85,
          "breakdown": "Authentication, reporting dashboard, and API integration require moderate effort.",
          "reasoning": "Estimated using fallback mock response because Gemini quota exceeded."
        }
        """;
    }
    private void validateConfiguration() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new EstimationFailedException("Gemini API key is not configured");
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();

        ArrayNode contents = root.putArray("contents");
        ObjectNode content = contents.addObject();
        ArrayNode parts = content.putArray("parts");
        parts.addObject().put("text", prompt);

        ObjectNode generationConfig = root.putObject("generationConfig");
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 2048);

        return objectMapper.writeValueAsString(root);
    }

    private String extractResponseText(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);

        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            JsonNode error = root.path("error").path("message");
            if (!error.isMissingNode()) {
                throw new EstimationFailedException("Gemini API error: " + error.asText());
            }
            throw new EstimationFailedException("Gemini response did not contain any candidates");
        }

        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new EstimationFailedException("Gemini response did not contain estimation content");
        }

        String text = parts.get(0).path("text").asText();
        if (text.isBlank()) {
            throw new EstimationFailedException("Gemini returned empty estimation text");
        }

        return text;
    }
}
