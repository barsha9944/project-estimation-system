package com.projectestimation.backend.common.exception;

import java.io.IOException;

public class ProposalFailedException extends RuntimeException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8156948143994929361L;

	public ProposalFailedException(String message) {
        super(message);
    }

    public ProposalFailedException(String message, Throwable cause) {
        super(message, cause);
    }
    
   
}
