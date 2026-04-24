package com.hjo2oa.data.common.interfaces.web;

import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.web.ApiErrorDetail;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.ResponseMeta;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.hjo2oa.data")
public class DataServicesGlobalExceptionHandler {

    private final ResponseMetaFactory responseMetaFactory;

    public DataServicesGlobalExceptionHandler(ResponseMetaFactory responseMetaFactory) {
        this.responseMetaFactory = responseMetaFactory;
    }

    @ExceptionHandler(DataServicesException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataServicesException(
            DataServicesException ex,
            HttpServletRequest request
    ) {
        return buildResponse(ex.errorDescriptor(), ex.userMessage(), null, request);
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
                DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR.descriptor(),
                DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR.defaultMessage(),
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
                DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR.descriptor(),
                DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR.defaultMessage(),
                errors,
                request
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex, HttpServletRequest request) {
        return buildResponse(
                DataServicesErrorCode.DATA_COMMON_BAD_REQUEST.descriptor(),
                DataServicesErrorCode.DATA_COMMON_BAD_REQUEST.defaultMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception in data services layer", ex);
        return buildResponse(
                DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR.descriptor(),
                DataServicesErrorCode.DATA_COMMON_INTERNAL_ERROR.defaultMessage(),
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
