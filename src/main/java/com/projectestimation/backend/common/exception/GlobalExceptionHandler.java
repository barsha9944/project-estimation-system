package com.projectestimation.backend.common.exception;

import com.projectestimation.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LogManager.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed at {}: {}", request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(ApiResponse.failure("Validation failed", errors));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.failure(ex.getMessage(), null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        String message = resolveCauseMessage(ex, "Invalid request body");
        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(ApiResponse.failure(message, null));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            log.warn("Invalid enum value for '{}' at {}", ex.getName(), request.getRequestURI());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("Invalid enum value for " + ex.getName(), null));
        }
        log.warn("Invalid request parameter '{}' at {}", ex.getName(), request.getRequestURI());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Invalid request parameter", null));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
        ConstraintViolationException ex,
        HttpServletRequest request
    ) {
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResponse.<Object>failure("Validation failed", ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        log.warn("Missing request parameter '{}' at {}", ex.getParameterName(), request.getRequestURI());
        return ResponseEntity.badRequest()
                .body(ApiResponse.failure("Missing required parameter: " + ex.getParameterName(), null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        log.warn("Method {} not supported at {}", ex.getMethod(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.failure("Request method is not supported", null));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(ex.getMessage(), null));
    }

    @ExceptionHandler(EstimationFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleEstimationFailed(
            EstimationFailedException ex,
            HttpServletRequest request
    ) {
        log.error("Estimation failed at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage(), null));
    }

    @ExceptionHandler(ProposalFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleProposalFailed(
            ProposalFailedException ex,
            HttpServletRequest request
    ) {
        log.error("Proposal failed at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage(), null));
    }

    @ExceptionHandler(AiGenerationFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAiGenerationFailed(
            AiGenerationFailedException ex,
            HttpServletRequest request
    ) {
        log.error("AI generation failed at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage(), null));
    }

    @ExceptionHandler(ProjectScheduleFailedException.class)
    public ResponseEntity<ApiResponse<Object>> handleProjectScheduleFailed(
            ProjectScheduleFailedException ex,
            HttpServletRequest request
    ) {
        log.error("Project schedule failed at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure(ex.getMessage(), null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Data integrity violation at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure("Data integrity violation", null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataAccess(
            DataAccessException ex,
            HttpServletRequest request
    ) {
        log.error("Database error at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Database operation failed", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("Something went wrong", null));
    }

    private String resolveCauseMessage(Throwable ex, String defaultMessage) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof BadRequestException badRequestException) {
                return badRequestException.getMessage();
            }
            current = current.getCause();
        }
        return defaultMessage;
    }
}
