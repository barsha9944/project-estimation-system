package com.projectestimation.backend.projectschedule.ai;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ProjectScheduleFailedException;
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

    try {

        return objectMapper.readValue(
                json,
                ProjectScheduleResponse.class
        );

    } catch (Exception ex) {

        throw new ProjectScheduleFailedException(
                "Failed to parse Gemini response",
                ex
        );

    }

}

}
