package com.projectestimation.backend.common.exception;

public class AiGenerationFailedException extends RuntimeException {

    public AiGenerationFailedException(String message) {
        super(message);
    }

    public AiGenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
