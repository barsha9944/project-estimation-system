package com.projectestimation.backend.common.ai;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.estimation.config.GeminiProperties;
import com.projectestimation.backend.opportunity.dto.GeminiFile;

@Component
public class GeminiClient {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	private final GeminiProperties properties;

	public GeminiClient(@Qualifier("geminiRestTemplate") RestTemplate restTemplate, ObjectMapper objectMapper,
			GeminiProperties properties) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	public String generateJsonContent(String prompt) {
		return generateContent(prompt, "application/json", 2048);
	}

	public String generateJsonContent(String prompt, int maxOutputTokens) {
		return generateContent(prompt, "application/json", maxOutputTokens);
	}

//    public String generateContent(String prompt, String responseMimeType, int maxOutputTokens) {
//        validateConfiguration();
//
//        try {
//            String url = properties.getBaseUrl() + "/models/" + properties.getModel() + ":generateContent";
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("x-goog-api-key", properties.getApiKey());
//
//            HttpEntity<String> request = new HttpEntity<>(
//                    buildRequestBody(prompt, responseMimeType, maxOutputTokens),
//                    headers
//            );
//            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
//
//            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
//                throw new AiGenerationFailedException("Gemini API returned an unsuccessful response");
//            }
//
//            return extractResponseText(response.getBody());
//        } catch (AiGenerationFailedException ex) {
//            throw ex;
//        } catch (RestClientException ex) {
//        	ex.printStackTrace();
//            throw new AiGenerationFailedException("Gemini API request failed or timed out", ex);
//        } catch (Exception ex) {
//            throw new AiGenerationFailedException("Unexpected error while calling Gemini API", ex);
//        }
//    }

	public String generateContent(String prompt, String responseMimeType, int maxOutputTokens) {

		return generateContent(properties.getModel(), prompt, responseMimeType, maxOutputTokens);
	}

	public String generateContentWithImages(String model, String prompt, List<Path> imagePaths, String responseMimeType,
			int maxOutputTokens) {

		validateConfiguration();

		try {

			List<Map<String, Object>> parts = new ArrayList<>();

			// Prompt part
			parts.add(Map.of("text", prompt));

			// Image parts
			for (Path imagePath : imagePaths) {

				byte[] imageBytes = Files.readAllBytes(imagePath);

				String base64 = Base64.getEncoder().encodeToString(imageBytes);

				parts.add(Map.of("inline_data", Map.of("mime_type", "image/png", "data", base64)));
			}

			Map<String, Object> generationConfig = new HashMap<>();

			generationConfig.put("temperature", 0.4);

			generationConfig.put("maxOutputTokens", maxOutputTokens);

			generationConfig.put("responseMimeType", responseMimeType);

			Map<String, Object> content = new HashMap<>();

			content.put("parts", parts);

			Map<String, Object> requestBody = new HashMap<>();

			requestBody.put("contents", List.of(content));

			requestBody.put("generationConfig", generationConfig);

//	        String url =
//	                properties.getBaseUrl()
//	                + "/models/"
//	                + properties.getModel()
//	                + ":generateContent";

			String url = properties.getBaseUrl() + "/models/" + model + ":generateContent";

			HttpHeaders headers = new HttpHeaders();

			headers.setContentType(MediaType.APPLICATION_JSON);

			headers.set("x-goog-api-key", properties.getApiKey());

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {

				throw new AiGenerationFailedException("Gemini API returned an unsuccessful response");
			}

			return extractResponseText(response.getBody());

		} catch (AiGenerationFailedException ex) {

			throw ex;

		} catch (HttpClientErrorException ex) {

			throw new AiGenerationFailedException(model, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);

		} catch (HttpServerErrorException ex) {

			throw new AiGenerationFailedException(model, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);

		} catch (ResourceAccessException ex) {

			throw new AiGenerationFailedException(model, 408, "Request timed out", ex);

		} catch (RestClientException ex) {

			throw new AiGenerationFailedException(model, 500, "Gemini API request failed", ex);

		} catch (Exception ex) {

			throw new AiGenerationFailedException(model, 500, "Unexpected Gemini error", ex);
		}
	}

	private void validateConfiguration() {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw new AiGenerationFailedException("Gemini API key is not configured");
		}
	}

	private String buildRequestBody(String prompt, String responseMimeType, int maxOutputTokens) throws Exception {
		ObjectNode root = objectMapper.createObjectNode();

		ArrayNode contents = root.putArray("contents");
		ObjectNode content = contents.addObject();
		ArrayNode parts = content.putArray("parts");
		parts.addObject().put("text", prompt);

		ObjectNode generationConfig = root.putObject("generationConfig");
		generationConfig.put("temperature", 0.3);
		generationConfig.put("maxOutputTokens", maxOutputTokens);
		generationConfig.put("responseMimeType", responseMimeType);

		return objectMapper.writeValueAsString(root);
	}

	private String extractResponseText(String responseBody) throws Exception {
		JsonNode root = objectMapper.readTree(responseBody);

		JsonNode candidates = root.path("candidates");
		if (!candidates.isArray() || candidates.isEmpty()) {
			JsonNode error = root.path("error").path("message");
			if (!error.isMissingNode()) {
				throw new AiGenerationFailedException("Gemini API error: " + error.asText());
			}
			throw new AiGenerationFailedException("Gemini response did not contain any candidates");
		}

		JsonNode parts = candidates.get(0).path("content").path("parts");
		if (!parts.isArray() || parts.isEmpty()) {
			throw new AiGenerationFailedException("Gemini response did not contain content");
		}

		String text = parts.get(0).path("text").asText();
		if (text.isBlank()) {
			throw new AiGenerationFailedException("Gemini returned empty content");
		}

		return text;
	}

	public String generateContent(String model, String prompt, String responseMimeType, int maxOutputTokens) {

		validateConfiguration();

		try {

			String url = properties.getBaseUrl() + "/models/" + model + ":generateContent";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-goog-api-key", properties.getApiKey());

			HttpEntity<String> request = new HttpEntity<>(buildRequestBody(prompt, responseMimeType, maxOutputTokens),
					headers);

			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {

				throw new AiGenerationFailedException("Gemini API returned an unsuccessful response");
			}

			return extractResponseText(response.getBody());

		} catch (AiGenerationFailedException ex) {

			throw ex;

		} catch (HttpClientErrorException ex) {

			throw new AiGenerationFailedException(model, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
		} catch (HttpServerErrorException ex) {

			throw new AiGenerationFailedException(model, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);
		} catch (ResourceAccessException ex) {

			throw new AiGenerationFailedException(model, 408, "Request timed out", ex);
		} catch (RestClientException ex) {

			throw new AiGenerationFailedException(model, 500, "Gemini API request failed", ex);
		} catch (Exception ex) {

			throw new AiGenerationFailedException(model, 500, "Unexpected Gemini error", ex);
		}
	}

	public String generateContentFromFile(String model, String fileReaderPrompt, GeminiFile fileUri,
			int proposalMaxOutputTokens) {
		validateConfiguration();

		try {

			List<Map<String, Object>> parts = new ArrayList<>();

			// Prompt part
			parts.add(Map.of("text", fileReaderPrompt));

			// Uploaded file part
			parts.add(
					Map.of("file_data", Map.of("mime_type", fileUri.getMimeType(), "file_uri", fileUri.getFileUri())));

			Map<String, Object> generationConfig = new HashMap<>();

			generationConfig.put("temperature", 0.4);

			generationConfig.put("maxOutputTokens", proposalMaxOutputTokens);

			generationConfig.put("responseMimeType", "text/plain");

			Map<String, Object> content = new HashMap<>();

			content.put("parts", parts);

			Map<String, Object> requestBody = new HashMap<>();

			requestBody.put("contents", List.of(content));

			requestBody.put("generationConfig", generationConfig);

			String url = properties.getBaseUrl() + "/models/" + model + ":generateContent";

			HttpHeaders headers = new HttpHeaders();

			headers.setContentType(MediaType.APPLICATION_JSON);

			headers.set("x-goog-api-key", properties.getApiKey());

			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new AiGenerationFailedException("Gemini API returned an unsuccessful response");
			}

			return extractResponseText(response.getBody());

		} catch (AiGenerationFailedException ex) {

			throw ex;

		} catch (HttpClientErrorException ex) {

			throw new AiGenerationFailedException(model, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);

		} catch (HttpServerErrorException ex) {

			throw new AiGenerationFailedException(model, ex.getStatusCode().value(), ex.getResponseBodyAsString(), ex);

		} catch (ResourceAccessException ex) {

			throw new AiGenerationFailedException(model, 408, "Request timed out", ex);

		} catch (RestClientException ex) {

			throw new AiGenerationFailedException(model, 500, "Gemini API request failed", ex);

		} catch (Exception ex) {

			throw new AiGenerationFailedException(model, 500, "Unexpected Gemini error", ex);
		}
	}
}
