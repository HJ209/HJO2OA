package com.hjo2oa.org.identity.context.interfaces;

import com.hjo2oa.org.identity.context.application.IdentityContextOperationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IdentityContextController.class)
public class IdentityContextExceptionHandler {

    @ExceptionHandler(IdentityContextOperationException.class)
    public ResponseEntity<IdentityContextErrorResponse> handle(IdentityContextOperationException ex) {
        return ResponseEntity.status(ex.httpStatus())
                .body(new IdentityContextErrorResponse(ex.errorCode(), ex.getMessage()));
    }
}
