package com.fastbills.api.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        List<ApiError.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::mapFieldError)
                .toList();

        ApiError error = new ApiError(
                ErrorCode.VALIDATION_FAILED,
                "Input validation failed",
                Instant.now(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
        ApiError error = new ApiError(
                ErrorCode.AUTHENTICATION_FAILED,
                ex.getMessage() != null ? ex.getMessage() : "Authentication failed"
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException ex) {
        ApiError error = new ApiError(
                ErrorCode.ACCESS_DENIED,
                ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Access denied for this resource"
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        ErrorCode code;
        if (status == HttpStatus.NOT_FOUND) {
            code = ErrorCode.RESOURCE_NOT_FOUND;
        } else if (status == HttpStatus.FORBIDDEN) {
            code = ErrorCode.ACCESS_DENIED;
        } else if (status == HttpStatus.UNAUTHORIZED) {
            code = ErrorCode.AUTHENTICATION_FAILED;
        } else if (status == HttpStatus.CONFLICT) {
            code = ErrorCode.DUPLICATE_RESOURCE;
        } else {
            code = ErrorCode.BUSINESS_RULE_VIOLATION;
        }

        ApiError error = new ApiError(
                code,
                ex.getReason() != null ? ex.getReason() : ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiError error = new ApiError(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        log.error("Unhandled exception caught by global handler", ex);
        ApiError error = new ApiError(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    private ApiError.FieldErrorDetail mapFieldError(FieldError fieldError) {
        return new ApiError.FieldErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value"
        );
    }
}
