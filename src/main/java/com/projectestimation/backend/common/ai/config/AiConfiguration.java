package com.projectestimation.backend.common.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.ai.OpenAiClient;
import com.projectestimation.backend.common.ai.gateway.AiProvider;
import com.projectestimation.backend.common.ai.gateway.OpenAiProvider;
import com.projectestimation.backend.common.ai.gemini.GeminiProvider;

@Configuration
public class AiConfiguration {

    @Bean
    public AiProvider geminiLiteProvider(
            GeminiClient client) {

        return new GeminiProvider(
                client,
                "Gemini Lite",
                "gemini-3.1-flash-lite",
                1
        );
    }

    @Bean
    public AiProvider openAiProvider(
            OpenAiClient client) {

        return new OpenAiProvider(
                client,
                "OpenAI",
                "gpt-4.1-mini",
                3
        );
    }
    
    
    @Bean
    public AiProvider geminiFlashProvider(
            GeminiClient client) {

        return new GeminiProvider(
                client,
                "Gemini Flash",
                "gemini-2.5-flash-lite",
                2
        );
    }
}