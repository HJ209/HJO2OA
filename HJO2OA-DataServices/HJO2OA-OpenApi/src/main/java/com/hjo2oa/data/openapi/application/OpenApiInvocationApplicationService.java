package com.hjo2oa.data.openapi.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.openapi.domain.AuthenticatedOpenApiInvocationContext;
import com.hjo2oa.data.openapi.domain.OpenApiHttpMethod;
import com.hjo2oa.data.openapi.infrastructure.OpenApiInvocationContextHolder;
import com.hjo2oa.data.service.application.DataServiceInvocationApplicationService;
import com.hjo2oa.data.service.application.DataServiceInvocationCommands;
import com.hjo2oa.data.service.domain.DataServiceViews;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OpenApiInvocationApplicationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final OpenApiInvocationContextHolder contextHolder;
    private final DataServiceInvocationApplicationService dataServiceInvocationApplicationService;
    private final ObjectMapper objectMapper;

    public OpenApiInvocationApplicationService(
            OpenApiInvocationContextHolder contextHolder,
            DataServiceInvocationApplicationService dataServiceInvocationApplicationService,
            ObjectMapper objectMapper
    ) {
        this.contextHolder = Objects.requireNonNull(contextHolder, "contextHolder must not be null");
        this.dataServiceInvocationApplicationService = Objects.requireNonNull(
                dataServiceInvocationApplicationService,
                "dataServiceInvocationApplicationService must not be null"
        );
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null")
                .copy()
                .findAndRegisterModules();
    }

    @OpenApiQuotaChecked
    public Map<String, Object> invoke(OpenApiInvocationRequest request) {
        AuthenticatedOpenApiInvocationContext context = contextHolder.getRequired();
        Map<String, Object> parameters = new LinkedHashMap<>(request.queryParameters());
        if (request.requestBody() != null && !request.requestBody().isBlank()) {
            Object parsedBody = parseBody(request.requestBody());
            if (parsedBody instanceof Map<?, ?> mapBody) {
                mapBody.forEach((key, value) -> parameters.put(String.valueOf(key), value));
            } else {
                parameters.put("_body", parsedBody);
            }
        }

        DataServiceViews.ExecutionPlan executionPlan = context.endpoint().httpMethod() == OpenApiHttpMethod.GET
                ? dataServiceInvocationApplicationService.query(new DataServiceInvocationCommands.InvocationCommand(
                        context.endpoint().dataServiceCode(),
                        "open-api",
                        context.clientCode(),
                        context.requestId(),
                        parameters
                ))
                : dataServiceInvocationApplicationService.submit(new DataServiceInvocationCommands.InvocationCommand(
                        context.endpoint().dataServiceCode(),
                        "open-api",
                        context.clientCode(),
                        context.requestId(),
                        parameters
                ));

        Map<String, Object> payload = objectMapper.convertValue(executionPlan, MAP_TYPE);
        payload.put("openApiCode", context.endpoint().code());
        payload.put("openApiVersion", context.endpoint().version());
        return Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    private Object parseBody(String requestBody) {
        try {
            return objectMapper.readValue(requestBody, Object.class);
        } catch (Exception ignored) {
            return requestBody;
        }
    }
}
