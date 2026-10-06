package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record IntroductionSddDto(
        String purposeOfDocument,
        List<ScopeTaskDto> scopeOfProposedSystem,
        List<DesignAlternativeDto> designAlternativesConsidered,
        String designAlternativesDecision,
        String makeBuyOrReuseDecisionsTaken
) {}