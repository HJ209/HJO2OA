package com.hjo2oa.infra.security.infrastructure;

import com.hjo2oa.infra.security.application.MaskingService;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.UseSharedWebContract;
import java.lang.reflect.Method;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = UseSharedWebContract.class)
public class SecurityMaskingResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final MaskingService maskingService;
    private final boolean responseMaskingEnabled;

    public SecurityMaskingResponseBodyAdvice(
            MaskingService maskingService,
            @Value("${hjo2oa.security.masking.response-enabled:true}") boolean responseMaskingEnabled
    ) {
        this.maskingService = Objects.requireNonNull(maskingService, "maskingService must not be null");
        this.responseMaskingEnabled = responseMaskingEnabled;
    }

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (!responseMaskingEnabled) {
            return false;
        }
        Method method = returnType.getMethod();
        Class<?> declaringClass = returnType.getDeclaringClass();
        return method != null
                && ApiResponse.class.isAssignableFrom(method.getReturnType())
                && declaringClass.isAnnotationPresent(UseSharedWebContract.class);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (!(body instanceof ApiResponse<?> apiResponse)) {
            return body;
        }
        return new ApiResponse<>(
                apiResponse.code(),
                apiResponse.message(),
                maskingService.maskResponseData(apiResponse.data()),
                apiResponse.errors(),
                apiResponse.meta()
        );
    }
}
