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
import org.springframework.http.client.SimpleClientHttpRequestFactory;

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
//            ResponseEntity<String> response = postToGemini(url, request);
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

			ResponseEntity<String> response = postToGemini(url, request);

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

			ResponseEntity<String> response = postToGemini(url, request);

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

			ResponseEntity<String> response = postToGemini(url, request);

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

    /**
     * Uploads a classpath/local document to the Gemini Files API and then
     * generates JSON using that uploaded file as an actual Gemini input.
     *
     * The uploaded file is not merely mentioned in the prompt; Gemini receives
     * the file URI as file_data, so the model can inspect the document.
     */
    public String generateJsonContentWithFile(
            String prompt,
            byte[] fileBytes,
            String displayName,
            String mimeType,
            int maxOutputTokens
    ) {
        validateConfiguration();

        try {
            String uploadUrl = properties.getBaseUrl()
                    .replace("/v1beta", "")
                    + "/upload/v1beta/files";

            // Step 1: initialize the resumable upload.
            HttpHeaders startHeaders = new HttpHeaders();
            startHeaders.setContentType(MediaType.APPLICATION_JSON);
            startHeaders.set("x-goog-api-key", properties.getApiKey());
            startHeaders.set("X-Goog-Upload-Protocol", "resumable");
            startHeaders.set("X-Goog-Upload-Command", "start");
            startHeaders.set("X-Goog-Upload-Header-Content-Length",
                    String.valueOf(fileBytes.length));
            startHeaders.set("X-Goog-Upload-Header-Content-Type", mimeType);

            Map<String, Object> metadata = Map.of(
                    "file", Map.of("display_name", displayName)
            );

            HttpEntity<Map<String, Object>> startRequest =
                    new HttpEntity<>(metadata, startHeaders);

            ResponseEntity<String> startResponse =
                    restTemplate.postForEntity(uploadUrl, startRequest, String.class);

            String resumableUploadUrl = startResponse.getHeaders()
                    .getFirst("X-Goog-Upload-URL");

            if (resumableUploadUrl == null || resumableUploadUrl.isBlank()) {
                throw new AiGenerationFailedException(
                        "Gemini Files API did not return an upload URL"
                );
            }

            // Step 2: upload the actual file bytes.
            HttpHeaders uploadHeaders = new HttpHeaders();
            uploadHeaders.setContentType(MediaType.parseMediaType(mimeType));
            uploadHeaders.set("X-Goog-Upload-Offset", "0");
            uploadHeaders.set("X-Goog-Upload-Command", "upload, finalize");

            HttpEntity<byte[]> uploadRequest =
                    new HttpEntity<>(fileBytes, uploadHeaders);

            ResponseEntity<String> uploadResponse =
                    restTemplate.postForEntity(
                            resumableUploadUrl,
                            uploadRequest,
                            String.class
                    );

            if (!uploadResponse.getStatusCode().is2xxSuccessful()
                    || uploadResponse.getBody() == null) {
                throw new AiGenerationFailedException(
                        "Gemini Files API upload failed"
                );
            }

            JsonNode uploadRoot =
                    objectMapper.readTree(uploadResponse.getBody());

            JsonNode fileNode = uploadRoot.path("file");
            String fileUri = fileNode.path("uri").asText();
            String uploadedMimeType = fileNode.path("mimeType").asText();

            if (fileUri == null || fileUri.isBlank()) {
                throw new AiGenerationFailedException(
                        "Gemini Files API did not return a file URI"
                );
            }

            if (uploadedMimeType == null || uploadedMimeType.isBlank()) {
                uploadedMimeType = mimeType;
            }

            return generateJsonContentFromFileUri(
                    prompt,
                    fileUri,
                    uploadedMimeType,
                    maxOutputTokens
            );

        } catch (AiGenerationFailedException ex) {
            throw ex;
        } catch (HttpClientErrorException ex) {
            throw new AiGenerationFailedException(
                    properties.getModel(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex
            );
        } catch (HttpServerErrorException ex) {
            throw new AiGenerationFailedException(
                    properties.getModel(),
                    ex.getStatusCode().value(),
                    ex.getResponseBodyAsString(),
                    ex
            );
        } catch (ResourceAccessException ex) {
            throw new AiGenerationFailedException(
                    properties.getModel(),
                    408,
                    "Request timed out while uploading or processing the Gemini reference file",
                    ex
            );
        } catch (RestClientException ex) {
            throw new AiGenerationFailedException(
                    properties.getModel(),
                    500,
                    "Gemini Files API request failed",
                    ex
            );
        } catch (Exception ex) {
            throw new AiGenerationFailedException(
                    properties.getModel(),
                    500,
                    "Unexpected error while uploading the Gemini reference file",
                    ex
            );
        }
    }

    private <T> ResponseEntity<String> postToGemini(String url, HttpEntity<T> request) {
        return restTemplate.postForEntity(url, request, String.class);
    }


    private String generateJsonContentFromFileUri(
            String prompt,
            String fileUri,
            String mimeType,
            int maxOutputTokens
    ) throws Exception {

        List<Map<String, Object>> parts = new ArrayList<>();

        parts.add(Map.of("text", prompt));
        parts.add(Map.of(
                "file_data",
                Map.of(
                        "mime_type", mimeType,
                        "file_uri", fileUri
                )
        ));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", maxOutputTokens);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> content = new HashMap<>();
        content.put("parts", parts);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("contents", List.of(content));
        requestBody.put("generationConfig", generationConfig);

        String url = properties.getBaseUrl()
                + "/models/"
                + properties.getModel()
                + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", properties.getApiKey());

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()
                || response.getBody() == null) {
            throw new AiGenerationFailedException(
                    "Gemini API returned an unsuccessful response"
            );
        }

        return extractResponseText(response.getBody());
    }

}
