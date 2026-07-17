package com.projectestimation.backend.projectschedule.ai;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiProjectScheduleOrchestrator {

    private static final int MAX_OUTPUT_TOKENS = 8192;

    private final GeminiProjectSchedulePromptBuilder promptBuilder;

    private final GeminiProjectScheduleResponseParser responseParser;

    private final GeminiClient geminiClient;

    public GeminiProjectScheduleOrchestrator(

            GeminiProjectSchedulePromptBuilder promptBuilder,

            GeminiProjectScheduleResponseParser responseParser,

            GeminiClient geminiClient

    ) {

        this.promptBuilder = promptBuilder;

        this.responseParser = responseParser;

        this.geminiClient = geminiClient;

    }

    public AiProjectScheduleResult generate(

            Opportunity opportunity,
            
            EstimationAnalysis analysis,

            String actors,

            String useCases,

            String projectStartDate,

            Integer teamSize,

            Integer workingDays,

            Integer workingHours,

            Integer buffer,
            
            Integer durationDays,
            
            Double estimatedHours

    ) {

        String prompt =
                promptBuilder.build(

                        opportunity,
                        
                        analysis,

                        actors,

                        useCases,

                        projectStartDate,

                        teamSize,

                        workingDays,

                        workingHours,

                        buffer,
                        
                        durationDays,
                        
                        estimatedHours

                );

        String json =
                geminiClient.generateJsonContent(
                        prompt,
                        MAX_OUTPUT_TOKENS
                );

        return new AiProjectScheduleResult(

                responseParser.parse(
                        json
                )

        );

    }
}