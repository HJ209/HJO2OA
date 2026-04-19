package com.hjo2oa.org.identity.context.application;

import org.springframework.http.HttpStatus;

public class IdentityContextOperationException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String errorCode;

    private IdentityContextOperationException(HttpStatus httpStatus, String errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public static IdentityContextOperationException conflict(String errorCode, String message) {
        return new IdentityContextOperationException(HttpStatus.CONFLICT, errorCode, message);
    }

    public static IdentityContextOperationException forbidden(String errorCode, String message) {
        return new IdentityContextOperationException(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }
}
