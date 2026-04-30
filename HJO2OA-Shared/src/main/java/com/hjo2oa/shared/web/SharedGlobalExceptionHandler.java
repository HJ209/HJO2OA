package com.hjo2oa.shared.web;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = UseSharedWebContract.class)
public class SharedGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SharedGlobalExceptionHandler.class);

    private final ResponseMetaFactory responseMetaFactory;
    private final ErrorMessageResolver errorMessageResolver;

    public SharedGlobalExceptionHandler(ResponseMetaFactory responseMetaFactory) {
        this(responseMetaFactory, ErrorMessageResolver.noop());
    }

    @Autowired
    public SharedGlobalExceptionHandler(
            ResponseMetaFactory responseMetaFactory,
            ObjectProvider<ErrorMessageResolver> errorMessageResolverProvider
    ) {
        this(
                responseMetaFactory,
                errorMessageResolverProvider.getIfAvailable(ErrorMessageResolver::noop)
        );
    }

    public SharedGlobalExceptionHandler(
            ResponseMetaFactory responseMetaFactory,
            ErrorMessageResolver errorMessageResolver
    ) {
        this.responseMetaFactory = responseMetaFactory;
        this.errorMessageResolver = errorMessageResolver;
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

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                SharedErrorDescriptors.UNAUTHORIZED,
                SharedErrorDescriptors.UNAUTHORIZED.defaultMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                SharedErrorDescriptors.FORBIDDEN,
                SharedErrorDescriptors.FORBIDDEN.defaultMessage(),
                null,
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

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        if (ex.getMessage() != null && ex.getMessage().contains("Tenant context is missing")) {
            return buildResponse(
                    SharedErrorDescriptors.TENANT_REQUIRED,
                    SharedErrorDescriptors.TENANT_REQUIRED.defaultMessage(),
                    null,
                    request
            );
        }
        log.error("Illegal state for request {}", request == null ? null : request.getRequestURI(), ex);
        return buildResponse(
                SharedErrorDescriptors.INTERNAL_ERROR,
                SharedErrorDescriptors.INTERNAL_ERROR.defaultMessage(),
                null,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnhandled(Exception ex, HttpServletRequest request) {
        if (hasTenantMissingCause(ex)) {
            return buildResponse(
                    SharedErrorDescriptors.TENANT_REQUIRED,
                    SharedErrorDescriptors.TENANT_REQUIRED.defaultMessage(),
                    null,
                    request
            );
        }
        log.error("Unhandled exception for request {}", request == null ? null : request.getRequestURI(), ex);
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
        String resolvedMessage = Optional.ofNullable(errorMessageResolver.resolve(descriptor, message, request)
                        .orElse(null))
                .filter(value -> !value.isBlank())
                .orElse(message);
        ApiResponse<Void> body = ApiResponse.failure(descriptor, resolvedMessage, errors, meta);
        return ResponseEntity.status(descriptor.httpStatus()).body(body);
    }

    private boolean hasTenantMissingCause(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            String message = cursor.getMessage();
            if (message != null && message.contains("Tenant context is missing")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private ApiErrorDetail toFieldErrorDetail(FieldError error) {
        return new ApiErrorDetail(error.getField(), error.getDefaultMessage(), error.getRejectedValue());
    }
}
