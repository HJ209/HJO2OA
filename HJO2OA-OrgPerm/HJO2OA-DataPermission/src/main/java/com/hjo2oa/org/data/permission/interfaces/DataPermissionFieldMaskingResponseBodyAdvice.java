package com.hjo2oa.org.data.permission.interfaces;

import com.hjo2oa.org.data.permission.application.DataPermissionApplicationService;
import com.hjo2oa.org.data.permission.application.DataPermissionCommands;
import com.hjo2oa.org.data.permission.domain.FieldPermissionRuntimeMasker;
import com.hjo2oa.org.data.permission.infrastructure.DataPermissionRuntimeContext;
import com.hjo2oa.shared.web.ApiResponse;
import com.hjo2oa.shared.web.UseSharedWebContract;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(annotations = UseSharedWebContract.class)
public class DataPermissionFieldMaskingResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final DataPermissionApplicationService applicationService;
    private final FieldPermissionRuntimeMasker fieldPermissionRuntimeMasker;
    private final boolean enabled;

    public DataPermissionFieldMaskingResponseBodyAdvice(
            DataPermissionApplicationService applicationService,
            FieldPermissionRuntimeMasker fieldPermissionRuntimeMasker,
            @Value("${hjo2oa.data-permission.field-response-masking-enabled:true}") boolean enabled
    ) {
        this.applicationService = Objects.requireNonNull(applicationService, "applicationService must not be null");
        this.fieldPermissionRuntimeMasker = Objects.requireNonNull(
                fieldPermissionRuntimeMasker,
                "fieldPermissionRuntimeMasker must not be null"
        );
        this.enabled = enabled;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Method method = returnType.getMethod();
        return enabled
                && method != null
                && ApiResponse.class.isAssignableFrom(method.getReturnType())
                && returnType.getDeclaringClass().isAnnotationPresent(UseSharedWebContract.class);
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
        DataPermissionRuntimeContext context = DataPermissionRuntimeContext.current();
        if (context == null || !(body instanceof ApiResponse<?> apiResponse)) {
            return body;
        }
        Object maskedData = maskData(apiResponse.data(), context);
        if (maskedData == apiResponse.data()) {
            return body;
        }
        return new ApiResponse<>(
                apiResponse.code(),
                apiResponse.message(),
                maskedData,
                apiResponse.errors(),
                apiResponse.meta()
        );
    }

    private Object maskData(Object data, DataPermissionRuntimeContext context) {
        if (data instanceof Map<?, ?> row) {
            return maskRow(toStringKeyMap(row), context);
        }
        if (data instanceof List<?> rows && rows.stream().allMatch(Map.class::isInstance)) {
            return rows.stream()
                    .map(row -> maskRow(toStringKeyMap((Map<?, ?>) row), context))
                    .toList();
        }
        return data;
    }

    private Map<String, Object> maskRow(Map<String, Object> row, DataPermissionRuntimeContext context) {
        var decision = applicationService.decideField(new DataPermissionCommands.FieldDecisionQuery(
                context.businessObject(),
                "view",
                context.tenantId(),
                fieldCodes(row),
                context.subjects()
        ));
        return fieldPermissionRuntimeMasker.apply(decision, row);
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> row) {
        return row.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (first, second) -> second,
                        java.util.LinkedHashMap::new
                ));
    }

    private List<String> fieldCodes(Map<String, Object> row) {
        Set<String> fields = new LinkedHashSet<>(row.keySet());
        return fields.stream().toList();
    }
}
