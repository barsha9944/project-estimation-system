package com.projectestimation.backend.dar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DarResponseDto(
        Long id,
        Long opportunityId,
        String projectPhase,
        String phaseJustification,
        LocalDate meetingDate,
        String meetingName,
        List<String> invitees,
        List<DarAgendaItemDto> agenda,
        List<DarDiscussionNoteDto> discussionNotes,
        List<String> evaluationCriteria,
        List<DarDecisionAlternativeDto> alternatives,
        String recommendedAlternativeId,
        String recommendationReason,
        String documentName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
