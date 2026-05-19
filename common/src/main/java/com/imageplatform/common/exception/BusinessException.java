package com.imageplatform.common.exception;

import org.springframework.http.HttpStatus;

// For violations of business rules (e.g. cancelling an already-completed job).
// Carries an HTTP status so the global handler can map it correctly.
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
