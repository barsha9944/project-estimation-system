package com.projectestimation.backend.projectschedule.service;

import java.util.Set;

public record ProjectScheduleSavedEvent(
        Long opportunityId,
        Set<Integer> affectedPsrVersions
) {
}