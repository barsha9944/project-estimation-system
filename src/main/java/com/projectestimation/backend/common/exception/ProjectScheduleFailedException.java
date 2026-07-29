package com.projectestimation.backend.common.exception;

public class ProjectScheduleFailedException extends RuntimeException {

    public ProjectScheduleFailedException(String message) {
        super(message);
    }

    public ProjectScheduleFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
