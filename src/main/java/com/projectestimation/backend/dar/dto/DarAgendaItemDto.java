package com.projectestimation.backend.dar.dto;

public record DarAgendaItemDto(
        String topic,
        String description,
        Integer durationMinutes
) {
}