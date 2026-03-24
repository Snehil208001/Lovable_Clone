package com.snehil.project.lovable_clone.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST,ex.getMessage());
        log.error(apiError.toString(),ex);
        return ResponseEntity.status(apiError.status()).body(apiError);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex) {
        String message = ex.getResourceName() + " with id " + ex.getResourceId() + " not found";
        ApiError apiError = new ApiError(HttpStatus.NOT_FOUND, message);

        log.error(apiError.toString(), ex);
        return ResponseEntity.status(apiError.status()).body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInputValidationError(MethodArgumentNotValidException ex) {
        List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST,"Input Validation Failed", errors);
        log.error(apiError.toString(),ex);
        return ResponseEntity.status(apiError.status()).body(apiError);
    }

    // --- NEWLY ADDED PRODUCTION HANDLERS BELOW ---

    // 1. The Global "Catch-All" (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllUncaughtException(Exception ex) {
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred");
        log.error("Unknown error occurred", ex);
        return ResponseEntity.status(apiError.status()).body(apiError);
    }

    // 2. Malformed JSON Requests (400 Bad Request)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJsonRequest(HttpMessageNotReadableException ex) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
        log.error("Malformed JSON: {}", ex.getMessage());
        return ResponseEntity.status(apiError.status()).body(apiError);
    }

    // 3. Database Integrity Violations (409 Conflict) - Handles duplicate emails
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ApiError apiError = new ApiError(HttpStatus.CONFLICT, "Database conflict: This record might already exist (e.g., duplicate email)");
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(apiError.status()).body(apiError);
    }

    // 4. Method Not Supported (405 Method Not Allowed)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String message = "Request method '" + ex.getMethod() + "' is not supported for this endpoint.";
        ApiError apiError = new ApiError(HttpStatus.METHOD_NOT_ALLOWED, message);
        // Using warn instead of error since this is just a bad client request, not a server crash
        log.warn("Method not supported: {}", message);
        return ResponseEntity.status(apiError.status()).body(apiError);
    }
}