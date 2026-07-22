package com.projectestimation.backend.projectschedule.ai;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleResponse;

@Component
public class GeminiProjectScheduleResponseParser {

    private final ObjectMapper objectMapper;

    public GeminiProjectScheduleResponseParser(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    public ProjectScheduleResponse parse(String json) {

    System.out.println("========== GEMINI RESPONSE ==========");
    System.out.println(json);
    System.out.println("=====================================");

    try {

        return objectMapper.readValue(
                json,
                ProjectScheduleResponse.class
        );

    } catch (Exception ex) {

        ex.printStackTrace();

        throw new RuntimeException(
                "Failed to parse Gemini response",
                ex
        );

    }

}

}