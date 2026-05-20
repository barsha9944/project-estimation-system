package com.projectestimation.backend.common.exception;

public class ProposalFailedException extends RuntimeException {

    public ProposalFailedException(String message) {
        super(message);
    }

    public ProposalFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
