package com.projectestimation.backend.common.ai.gateway;

import java.nio.file.Path;
import java.util.List;

import com.projectestimation.backend.opportunity.dto.GeminiFile;

public interface AiProvider {

	String getName();

	int getPriority();

	String generateContent(String prompt, String responseMimeType, int maxOutputTokens);

	String generateContentWithImages(String prompt, List<Path> imagePaths, String responseMimeType,
			int maxOutputTokens);

	default String generateContentFromFile(String fileReaderPrompt, GeminiFile fileUri, int proposalMaxOutputTokens) {
		return null;
	}

}