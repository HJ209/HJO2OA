package com.hjo2oa.shared.web;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = UseSharedWebContract.class)
public class SharedGlobalExceptionHandler {

    private final ResponseMetaFactory responseMetaFactory;

    public SharedGlobalExceptionHandler(ResponseMetaFactory responseMetaFactory) {
        this.responseMetaFactory = responseMetaFactory;
    }

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException ex, HttpServletRequest request) {
        return buildResponse(ex.errorDescriptor(), ex.userMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorDetail> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldErrorDetail)
                .collect(Collectors.toList());
        return buildResponse(
                SharedErrorDescriptors.VALIDATION_ERROR,
                SharedErrorDescriptors.VALIDATION_ERROR.defaultMessage(),
                errors,
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ApiErrorDetail> errors = ex.getConstraintViolations()
                .stream()
                .map(violation -> new ApiErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage(),
                        violation.getInvalidValue()))
                .collect(Collectors.toList());
        return buildResponse(
                SharedErrorDescriptors.VALIDATION_ERROR,
                SharedErrorDescriptors.VALIDATION_ERROR.defaultMessage(),
                errors,
                request
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return buildResponse(
                SharedErrorDescriptors.BAD_REQUEST,
                SharedErrorDescriptors.BAD_REQUEST.defaultMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex, HttpServletRequest request) {
        return buildResponse(
                SharedErrorDescriptors.INTERNAL_ERROR,
                SharedErrorDescriptors.INTERNAL_ERROR.defaultMessage(),
                null,
                request
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
            ErrorDescriptor descriptor,
            String message,
            List<ApiErrorDetail> errors,
            HttpServletRequest request
    ) {
        ResponseMeta meta = responseMetaFactory.create(request);
        ApiResponse<Void> body = ApiResponse.failure(descriptor, message, errors, meta);
        return ResponseEntity.status(descriptor.httpStatus()).body(body);
    }

    private ApiErrorDetail toFieldErrorDetail(FieldError error) {
        return new ApiErrorDetail(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }
}
