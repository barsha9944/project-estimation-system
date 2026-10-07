package com.projectestimation.backend.mom.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MomDto(
        Long id,
        Long opportunityId,
        String projectName,
        String clientName,
        String meetingType,
        Integer meetingSequence,
        LocalDate meetingDate,
        String meetingName,
        String meetingTime,
        String meetingLocation,
        List<String> invitees,
        String recordedBy,
        String circulation,
        List<MomAgendaItemDto> agenda,
        List<String> discussionNotes,
        String documentName,
        LocalDateTime createdAt
) {
}