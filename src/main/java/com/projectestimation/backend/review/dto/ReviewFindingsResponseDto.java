package com.projectestimation.backend.review.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewFindingsResponseDto(
    List<ReviewFindingDto> findings,
    String summary,
    Boolean approved
) {}
