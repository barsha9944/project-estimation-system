package com.projectestimation.backend.common.ai.gateway;

import java.time.Instant;

public class ProviderStatus {

    private Instant unavailableUntil;

    public boolean isAvailable() {

        return unavailableUntil == null
                || Instant.now().isAfter(unavailableUntil);
    }

    public void markUnavailable(int seconds) {

        unavailableUntil =
                Instant.now().plusSeconds(seconds);
    }

    public void clear() {

        unavailableUntil = null;
    }
}