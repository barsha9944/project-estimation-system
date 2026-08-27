package com.projectestimation.backend.opportunity.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.estimation.config.GeminiProperties;
import com.projectestimation.backend.opportunity.dto.GeminiFile;

@Service
public class GeminiFileService {

	private final RestTemplate restTemplate = new RestTemplate();

	private final GeminiProperties properties;
	private final ObjectMapper objectMapper;

	public GeminiFileService(

			GeminiProperties properties, ObjectMapper objectMapper) {

		this.properties = properties;
		this.objectMapper = objectMapper;
	}

	@Autowired
	Environment environment;

	public GeminiFile uploadFile(Path filePath) throws IOException {
		GeminiFile geminiFile = new GeminiFile();
		final String apiKey = properties.getApiKey();

		String fileName = filePath.getFileName().toString();
		byte[] fileBytes = Files.readAllBytes(filePath);
		String mimeType = Files.probeContentType(filePath);

		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}

		long fileSize = Files.size(filePath);

		// 1. Start resumable upload
		HttpHeaders startHeaders = new HttpHeaders();
		startHeaders.set("X-Goog-Upload-Protocol", "resumable");
		startHeaders.set("X-Goog-Upload-Command", "start");
		startHeaders.set("X-Goog-Upload-Header-Content-Length", String.valueOf(fileSize));
		startHeaders.set("X-Goog-Upload-Header-Content-Type", mimeType);
		startHeaders.setContentType(MediaType.APPLICATION_JSON);

		String metadata = """
				{
				  "file": {
				    "display_name": "%s"
				  }
				}
				""".formatted(fileName);

		HttpEntity<String> startRequest = new HttpEntity<>(metadata, startHeaders);

		ResponseEntity<String> startResponse = restTemplate.exchange(
				"https://generativelanguage.googleapis.com/upload/v1beta/files?key=" + apiKey, HttpMethod.POST,
				startRequest, String.class);

		String uploadUrl = startResponse.getHeaders().getFirst("X-Goog-Upload-URL");

		if (uploadUrl == null) {
			throw new RuntimeException("Gemini upload URL not returned");
		}

		// 2. Upload the actual server file
		HttpHeaders uploadHeaders = new HttpHeaders();
		uploadHeaders.set("X-Goog-Upload-Offset", "0");
		uploadHeaders.set("X-Goog-Upload-Command", "upload, finalize");
		uploadHeaders.setContentType(MediaType.parseMediaType(mimeType));

		HttpEntity<byte[]> uploadRequest = new HttpEntity<>(fileBytes, uploadHeaders);

		ResponseEntity<String> uploadResponse = restTemplate.exchange(uploadUrl, HttpMethod.POST, uploadRequest,
				String.class);
		JsonNode root = objectMapper.readTree(uploadResponse.getBody());

		JsonNode fileNode = root.get("file");

		geminiFile.setFileUri(fileNode.get("uri").asText());

		geminiFile.setMimeType(fileNode.get("mimeType").asText());
		return geminiFile;
	}
}
