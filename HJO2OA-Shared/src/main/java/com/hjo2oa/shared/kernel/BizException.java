package com.hjo2oa.shared.kernel;

import org.springframework.http.HttpStatus;

public class BizException extends RuntimeException {

    private final ErrorDescriptor errorDescriptor;

    public BizException(ErrorDescriptor errorDescriptor) {
        super(errorDescriptor.defaultMessage());
        this.errorDescriptor = errorDescriptor;
    }

    public BizException(ErrorDescriptor errorDescriptor, String message) {
        super(message);
        this.errorDescriptor = errorDescriptor;
    }

    public BizException(ErrorDescriptor errorDescriptor, String message, Throwable cause) {
        super(message, cause);
        this.errorDescriptor = errorDescriptor;
    }

    public String errorCode() {
        return errorDescriptor.code();
    }

    public HttpStatus httpStatus() {
        return errorDescriptor.httpStatus();
    }

    public String userMessage() {
        String message = getMessage();
        return message == null || message.isBlank() ? errorDescriptor.defaultMessage() : message;
    }

    public ErrorDescriptor errorDescriptor() {
        return errorDescriptor;
    }
}
