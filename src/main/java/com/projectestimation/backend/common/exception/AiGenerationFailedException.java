package com.projectestimation.backend.common.exception;

public class AiGenerationFailedException extends RuntimeException {

    private final Integer statusCode;
    private final String provider;

    public AiGenerationFailedException(String message) {
        super(message);
        this.statusCode = null;
        this.provider = null;
    }

    public AiGenerationFailedException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.provider = null;
    }

    public AiGenerationFailedException(
            String provider,
            Integer statusCode,
            String message) {

        super(message);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public AiGenerationFailedException(
            String provider,
            Integer statusCode,
            String message,
            Throwable cause) {

        super(message, cause);
        this.provider = provider;
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getProvider() {
        return provider;
    }
}