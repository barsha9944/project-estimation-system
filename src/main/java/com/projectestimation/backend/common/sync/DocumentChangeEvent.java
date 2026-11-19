package com.projectestimation.backend.common.sync;

import java.time.LocalDateTime;

public record DocumentChangeEvent(
        Long opportunityId,
        String documentType,
        String filePath,
        String changeType,
        String fileHash,
        LocalDateTime timestamp,
        String message
) {
    public static DocumentChangeEvent of(
            Long opportunityId,
            String documentType,
            String filePath,
            String changeType,
            String fileHash,
            String message
    ) {
        return new DocumentChangeEvent(
                opportunityId,
                documentType,
                filePath,
                changeType,
                fileHash,
                LocalDateTime.now(),
                message
        );
    }
}
