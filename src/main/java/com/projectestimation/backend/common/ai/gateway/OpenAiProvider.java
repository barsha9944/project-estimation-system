package com.projectestimation.backend.common.ai.gateway;

import java.nio.file.Path;
import java.util.List;

import com.projectestimation.backend.common.ai.OpenAiClient;

public class OpenAiProvider implements AiProvider {

    private final OpenAiClient openAiClient;
    private final String model;
    private final String name;
    private final int priority;

    public OpenAiProvider(
            OpenAiClient openAiClient,
            String name,
            String model,
            int priority) {

        this.openAiClient = openAiClient;
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
    public String generateContent(
            String prompt,
            String responseMimeType,
            int maxOutputTokens) {

        return openAiClient.generateContent(
                model,
                prompt,
                responseMimeType,
                maxOutputTokens
        );
    }

    @Override
    public String generateContentWithImages(
            String prompt,
            List<Path> imagePaths,
            String responseMimeType,
            int maxOutputTokens) {

        throw new UnsupportedOperationException(
                "OpenAI image input is not implemented yet."
        );
    }
}