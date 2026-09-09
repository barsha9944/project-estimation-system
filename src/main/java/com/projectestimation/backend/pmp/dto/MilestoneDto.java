package com.projectestimation.backend.pmp.dto;

public record MilestoneDto(

        String phase,

        String milestone,

        String description,

        String targetDate,

        String deliverable

) {
}