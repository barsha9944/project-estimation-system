package com.projectestimation.backend.common.ai;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.estimation.config.OpenAiProperties;

@Component
public class OpenAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties properties;

    public OpenAiClient(
            @Qualifier("openAiRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            OpenAiProperties properties) {

        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String generateContent(
            String model,
            String prompt,
            String responseMimeType,
            int maxOutputTokens) {

        validateConfiguration();

        try {

            String url =
                    properties.getBaseUrl()
                    + "/responses";

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.setBearerAuth(
                    properties.getApiKey()
            );

            HttpEntity<String> request =
                    new HttpEntity<>(
                            buildRequestBody(
                                    model,
                                    prompt,
                                    maxOutputTokens
                            ),
                            headers
                    );

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class
                    );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null) {

            	throw new AiGenerationFailedException(
            	        model,
            	        response.getStatusCode().value(),
            	        "OpenAI returned an unsuccessful response."
            	);
            }

            return extractResponseText(
                    response.getBody()
            );

        } catch (AiGenerationFailedException ex) {

            throw ex;

        } catch (HttpClientErrorException ex) {

            throw new AiGenerationFailedException(
                    model,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex
            );

        } catch (HttpServerErrorException ex) {

            throw new AiGenerationFailedException(
                    model,
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex
            );

        } catch (ResourceAccessException ex) {

            throw new AiGenerationFailedException(
                    model,
                    408,
                    "Request timed out",
                    ex
            );

        } catch (RestClientException ex) {

            throw new AiGenerationFailedException(
                    model,
                    500,
                    "OpenAI request failed",
                    ex
            );

        } catch (Exception ex) {

            throw new AiGenerationFailedException(
                    model,
                    500,
                    "Unexpected OpenAI error",
                    ex
            );
        }
    }

    private void validateConfiguration() {

        if (properties.getApiKey() == null
                || properties.getApiKey().isBlank()) {

        	throw new AiGenerationFailedException(
        	        "OpenAI",
        	        503,
        	        "OpenAI API key is not configured"
        	);
        }
    }

   private String buildRequestBody(
        String model,
        String prompt,
        int maxOutputTokens) throws Exception {

    var root = objectMapper.createObjectNode();

    root.put("model", model);
    root.put("input", prompt);

    var text = root.putObject("text");
    var format = text.putObject("format");
    format.put("type", "text");

    root.put("max_output_tokens", maxOutputTokens);

    return objectMapper.writeValueAsString(root);
}
    private String extractResponseText(
            String responseBody) throws Exception {

        var root = objectMapper.readTree(responseBody);

        if (root.has("error")) {

            throw new AiGenerationFailedException(
                    root.get("error").toString()
            );
        }

        if (root.has("output_text")) {

            String text = root.get("output_text").asText();

            if (text != null && !text.isBlank()) {
                return text;
            }
        }

        throw new AiGenerationFailedException(
                "OpenAI",
                500,
                "OpenAI response did not contain any output text."
        );
    }
}