package com.hjo2oa.org.identity.context.interfaces;

import com.hjo2oa.org.identity.context.application.IdentityContextOperationException;
import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = IdentityContextController.class)
public class IdentityContextExceptionHandler {

    private final ResponseMetaFactory responseMetaFactory;

    public IdentityContextExceptionHandler(ResponseMetaFactory responseMetaFactory) {
        this.responseMetaFactory = responseMetaFactory;
    }

    @ExceptionHandler(IdentityContextOperationException.class)
    public ResponseEntity<ApiResponse<Void>> handle(IdentityContextOperationException ex, HttpServletRequest request) {
        ErrorDescriptor descriptor = SharedErrorDescriptors.of(ex.errorCode(), ex.httpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.httpStatus())
                .body(ApiResponse.failure(descriptor, ex.getMessage(), null, responseMetaFactory.create(request)));
    }
}
