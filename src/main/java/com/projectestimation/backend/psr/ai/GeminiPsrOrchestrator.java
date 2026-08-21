package com.projectestimation.backend.psr.ai;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.psr.dto.PsrContentDto;

@Service
public class GeminiPsrOrchestrator {

    private static final int MAX_OUTPUT_TOKENS = 8192;

    private final GeminiPsrPromptBuilder promptBuilder;
    private final GeminiPsrResponseParser responseParser;
    private final GeminiClient geminiClient;

    public GeminiPsrOrchestrator(
            GeminiPsrPromptBuilder promptBuilder,
            GeminiPsrResponseParser responseParser,
            GeminiClient geminiClient
    ) {
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
        this.geminiClient = geminiClient;
    }

    public AiPsrResult generate(
            String opportunityName,
            PsrContentDto content,
            int psrVersion
    ) {

        String prompt = promptBuilder.build(
                opportunityName,
                content,
                psrVersion
        );

        String markdown = geminiClient.generateContent(
                prompt,
                "text/plain",
                MAX_OUTPUT_TOKENS
        );

        return new AiPsrResult(
                responseParser.parse(markdown)
        );
    }
}