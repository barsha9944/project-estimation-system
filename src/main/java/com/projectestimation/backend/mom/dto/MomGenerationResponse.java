package com.projectestimation.backend.mom.dto;

import java.util.List;

public record MomGenerationResponse(
        Long opportunityId,
        Integer totalMeetings,
        List<MomDto> meetings
) {
}