package com.projectestimation.backend.common.ai.gemini;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.ai.gateway.AiProvider;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.opportunity.dto.GeminiFile;

public class GeminiProvider implements AiProvider {

	private final GeminiClient geminiClient;
	private final String model;
	private final String name;
	private final int priority;

	public GeminiProvider(GeminiClient geminiClient, String name, String model, int priority) {

		this.geminiClient = geminiClient;
		this.name = name;
		this.model = model;
		this.priority = priority;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public String generateContent(String prompt, String responseMimeType, int maxOutputTokens) {

		return geminiClient.generateContent(model, prompt, responseMimeType, maxOutputTokens);
	}

	@Override
	public String generateContentWithImages(String prompt, List<Path> imagePaths, String responseMimeType,
			int maxOutputTokens) {

		return geminiClient.generateContentWithImages(model, prompt, imagePaths, responseMimeType, maxOutputTokens);
	}

	@Override
	public String generateContentFromFile(String fileReaderPrompt, GeminiFile fileUri, int proposalMaxOutputTokens) {
		return geminiClient.generateContentFromFile(model, fileReaderPrompt, fileUri, proposalMaxOutputTokens);
	}

}