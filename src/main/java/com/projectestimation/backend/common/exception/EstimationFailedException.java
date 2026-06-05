package com.projectestimation.backend.common.exception;

public class EstimationFailedException extends RuntimeException {

    public EstimationFailedException(String message) {
        super(message);
    }

    public EstimationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
