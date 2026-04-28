package com.hjo2oa.data.service.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import com.hjo2oa.data.service.domain.DataServiceDefinition;
import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.data.service.domain.DataServiceOperationContext;
import com.hjo2oa.data.service.domain.DataServiceOperationContextProvider;
import com.hjo2oa.data.service.domain.DataServiceViews;
import com.hjo2oa.data.service.domain.ServiceFieldMapping;
import com.hjo2oa.data.service.domain.ServiceParameterDefinition;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DataServiceInvocationApplicationService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Comparator<ServiceFieldMapping> FIELD_MAPPING_ORDER =
            Comparator.comparingInt(ServiceFieldMapping::sortOrder)
                    .thenComparing(ServiceFieldMapping::targetField)
                    .thenComparing(ServiceFieldMapping::sourceField);

    private final DataServiceDefinitionRepository repository;
    private final DataServiceOperationContextProvider contextProvider;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    @Autowired
    public DataServiceInvocationApplicationService(
            DataServiceDefinitionRepository repository,
            DataServiceOperationContextProvider contextProvider,
            ObjectMapper objectMapper
    ) {
        this(repository, contextProvider, objectMapper, Clock.systemUTC());
    }
    public DataServiceInvocationApplicationService(
            DataServiceDefinitionRepository repository,
            DataServiceOperationContextProvider contextProvider,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @DataAuditLog(module = "data-service", action = "prepare-query-execution", targetType = "DataServiceDefinition")
    public DataServiceViews.ExecutionPlan query(DataServiceInvocationCommands.InvocationCommand command) {
        return prepare(command, true);
    }

    @DataAuditLog(module = "data-service", action = "prepare-submit-execution", targetType = "DataServiceDefinition")
    public DataServiceViews.ExecutionPlan submit(DataServiceInvocationCommands.InvocationCommand command) {
        return prepare(command, false);
    }

    private DataServiceViews.ExecutionPlan prepare(
            DataServiceInvocationCommands.InvocationCommand command,
            boolean queryMode
    ) {
        Objects.requireNonNull(command, "command must not be null");
        DataServiceOperationContext context = currentExecutionContext();
        DataServiceDefinition definition = repository.findActiveByCode(context.tenantId(), command.serviceCode())
                .orElseThrow(() -> new DataServicesException(
                        DataServicesErrorCode.DATA_SERVICE_NOT_FOUND,
                        "Activated data service definition not found"
                ));
        validateServiceType(definition, queryMode);
        String effectiveAppCode = command.appCode();
        String effectiveSubjectId = command.subjectId();
        validatePermission(definition, context, effectiveAppCode, effectiveSubjectId);
        Map<String, Object> normalizedParameters = normalizeParameters(definition, command.parameters());
        String cacheKey = definition.cachePolicy().resolveCacheKey(
                definition.tenantId(),
                definition.code(),
                effectiveAppCode,
                effectiveSubjectId,
                normalizedParameters
        );
        List<DataServiceViews.FieldMappingView> outputMappings = definition.fieldMappings().stream()
                .sorted(FIELD_MAPPING_ORDER)
                .map(ServiceFieldMapping::toView)
                .toList();
        List<String> outputFields = outputMappings.stream()
                .map(DataServiceViews.FieldMappingView::targetField)
                .toList();
        return new DataServiceViews.ExecutionPlan(
                definition.serviceId(),
                definition.code(),
                definition.serviceType(),
                definition.sourceMode(),
                definition.permissionMode(),
                definition.cachePolicy().enabled(),
                cacheKey,
                definition.cachePolicy().ttlSeconds(),
                effectiveAppCode,
                effectiveSubjectId,
                command.idempotencyKey(),
                normalizedParameters,
                outputMappings,
                outputFields,
                definition.reusableByOpenApi(),
                definition.reusableByReport(),
                clock.instant()
        );
    }

    private DataServiceOperationContext currentExecutionContext() {
        DataServiceOperationContext context = Objects.requireNonNull(
                contextProvider.currentContext(),
                "contextProvider.currentContext() must not return null"
        );
        if (!context.internalAccess() && !context.canManageDefinitions() && !context.canViewDefinitions()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                    "Current operator is not allowed to invoke data service definitions"
            );
        }
        return context;
    }

    private void validateServiceType(DataServiceDefinition definition, boolean queryMode) {
        if (queryMode) {
            if (definition.serviceType() != DataServiceDefinition.ServiceType.QUERY
                    && definition.serviceType() != DataServiceDefinition.ServiceType.EXPORT) {
                throw new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_CONFLICT,
                        "Only QUERY or EXPORT service can use query interface"
                );
            }
            return;
        }
        if (definition.serviceType() != DataServiceDefinition.ServiceType.COMMAND
                && definition.serviceType() != DataServiceDefinition.ServiceType.CALLBACK) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_CONFLICT,
                    "Only COMMAND or CALLBACK service can use submit interface"
            );
        }
    }

    private void validatePermission(
            DataServiceDefinition definition,
            DataServiceOperationContext context,
            String appCode,
            String subjectId
    ) {
        if (!context.internalAccess()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                    "Data service invocation requires internal access context"
            );
        }
        List<String> requiredRoles = definition.permissionBoundary().requiredRoles();
        if (!requiredRoles.isEmpty() && requiredRoles.stream().noneMatch(context::hasRole)) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                    "Current operator does not satisfy required data service roles"
            );
        }
        if (context.canManageDefinitions()) {
            return;
        }
        if (definition.permissionMode() == DataServiceDefinition.PermissionMode.PUBLIC_INTERNAL) {
            return;
        }
        if (definition.permissionMode() == DataServiceDefinition.PermissionMode.APP_SCOPED) {
            if (appCode == null) {
                throw new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                        "appCode is required for APP_SCOPED data service"
                );
            }
            if (!definition.permissionBoundary().allowedAppCodes().contains(appCode)
                    || !context.authorizedAppCodes().contains(appCode)) {
                throw new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                        "Current operator is not allowed to use the requested app scope"
                );
            }
            return;
        }
        if (subjectId == null) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "subjectId is required for SUBJECT_SCOPED data service"
            );
        }
        if (!definition.permissionBoundary().allowedSubjectIds().contains(subjectId)
                || !context.authorizedSubjectIds().contains(subjectId)) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_FORBIDDEN,
                    "Current operator is not allowed to use the requested subject scope"
            );
        }
    }

    private Map<String, Object> normalizeParameters(
            DataServiceDefinition definition,
            Map<String, Object> rawParameters
    ) {
        Map<String, Object> providedParameters = rawParameters == null
                ? Map.of()
                : rawParameters;
        Set<String> expectedCodes = new LinkedHashSet<>();
        Map<String, Object> normalized = new LinkedHashMap<>();
        List<ServiceParameterDefinition> enabledParameters = definition.parameters().stream()
                .filter(ServiceParameterDefinition::enabled)
                .sorted(Comparator.comparingInt(ServiceParameterDefinition::sortOrder)
                        .thenComparing(ServiceParameterDefinition::paramCode))
                .toList();
        for (ServiceParameterDefinition parameter : enabledParameters) {
            expectedCodes.add(parameter.paramCode());
            Object rawValue = providedParameters.get(parameter.paramCode());
            Object effectiveValue = rawValue;
            if (effectiveValue == null && parameter.defaultValue() != null) {
                effectiveValue = parameter.defaultValue();
            }
            if (effectiveValue == null) {
                if (parameter.required()) {
                    throw new DataServicesException(
                            DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                            "Missing required parameter: " + parameter.paramCode()
                    );
                }
                continue;
            }
            Object normalizedValue = normalizeParameterValue(parameter, effectiveValue);
            normalized.put(parameter.paramCode(), normalizedValue);
        }
        for (String parameterCode : providedParameters.keySet()) {
            if (!expectedCodes.contains(parameterCode)) {
                throw new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                        "Unexpected parameter: " + parameterCode
                );
            }
        }
        return Map.copyOf(normalized);
    }

    private Object normalizeParameterValue(ServiceParameterDefinition parameter, Object rawValue) {
        ServiceParameterDefinition.ValidationRule rule = parameter.validationRule();
        return switch (parameter.paramType()) {
            case STRING -> normalizeString(parameter.paramCode(), rawValue, rule);
            case NUMBER -> normalizeNumber(parameter.paramCode(), rawValue, rule);
            case BOOLEAN -> normalizeBoolean(parameter.paramCode(), rawValue);
            case DATE -> normalizeDate(parameter.paramCode(), rawValue);
            case JSON -> normalizeJson(parameter.paramCode(), rawValue);
            case PAGEABLE -> normalizePageable(parameter.paramCode(), rawValue, rule);
        };
    }

    private String normalizeString(
            String paramCode,
            Object rawValue,
            ServiceParameterDefinition.ValidationRule rule
    ) {
        String normalized = String.valueOf(rawValue).trim();
        if (normalized.isEmpty()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter must not be blank: " + paramCode
            );
        }
        if (rule.minLength() != null && normalized.length() < rule.minLength()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter length is too short: " + paramCode
            );
        }
        if (rule.maxLength() != null && normalized.length() > rule.maxLength()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter length is too long: " + paramCode
            );
        }
        if (rule.regex() != null && !Pattern.compile(rule.regex()).matcher(normalized).matches()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter format is invalid: " + paramCode
            );
        }
        validateAllowedValues(paramCode, normalized, rule.allowedValues());
        return normalized;
    }

    private BigDecimal normalizeNumber(
            String paramCode,
            Object rawValue,
            ServiceParameterDefinition.ValidationRule rule
    ) {
        BigDecimal normalized;
        try {
            normalized = rawValue instanceof BigDecimal bigDecimal
                    ? bigDecimal
                    : new BigDecimal(String.valueOf(rawValue).trim());
        } catch (NumberFormatException exception) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter is not a valid number: " + paramCode,
                    exception
            );
        }
        if (rule.minValue() != null && normalized.compareTo(rule.minValue()) < 0) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter is smaller than allowed minimum: " + paramCode
            );
        }
        if (rule.maxValue() != null && normalized.compareTo(rule.maxValue()) > 0) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter is greater than allowed maximum: " + paramCode
            );
        }
        validateAllowedValues(paramCode, normalized.stripTrailingZeros().toPlainString(), rule.allowedValues());
        return normalized.stripTrailingZeros();
    }

    private Boolean normalizeBoolean(String paramCode, Object rawValue) {
        if (rawValue instanceof Boolean value) {
            return value;
        }
        String normalized = String.valueOf(rawValue).trim().toLowerCase(Locale.ROOT);
        if ("true".equals(normalized)) {
            return Boolean.TRUE;
        }
        if ("false".equals(normalized)) {
            return Boolean.FALSE;
        }
        throw new DataServicesException(
                DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                "Parameter is not a valid boolean: " + paramCode
        );
    }

    private String normalizeDate(String paramCode, Object rawValue) {
        String normalized = String.valueOf(rawValue).trim();
        if (normalized.isEmpty()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter must not be blank: " + paramCode
            );
        }
        if (!isIsoDateLike(normalized)) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter is not a supported ISO date or datetime: " + paramCode
            );
        }
        return normalized;
    }

    private Object normalizeJson(String paramCode, Object rawValue) {
        if (rawValue instanceof Map<?, ?> || rawValue instanceof List<?>) {
            return objectMapper.convertValue(rawValue, Object.class);
        }
        if (rawValue instanceof String stringValue) {
            try {
                return objectMapper.readValue(stringValue, Object.class);
            } catch (IOException exception) {
                throw new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                        "Parameter is not a valid JSON value: " + paramCode,
                        exception
                );
            }
        }
        return objectMapper.convertValue(rawValue, Object.class);
    }

    private Map<String, Object> normalizePageable(
            String paramCode,
            Object rawValue,
            ServiceParameterDefinition.ValidationRule rule
    ) {
        Map<String, Object> payload = rawValue instanceof String stringValue
                ? readPageableJson(paramCode, stringValue)
                : objectMapper.convertValue(rawValue, MAP_TYPE);
        Object pageValue = payload.get("page");
        Object sizeValue = payload.get("size");
        if (pageValue == null || sizeValue == null) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "PAGEABLE parameter must contain page and size: " + paramCode
            );
        }
        int page = toPositiveInt(paramCode + ".page", pageValue);
        int size = toPositiveInt(paramCode + ".size", sizeValue);
        if (rule.maxPageSize() != null && size > rule.maxPageSize()) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "PAGEABLE parameter size exceeds maxPageSize: " + paramCode
            );
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("page", page);
        normalized.put("size", size);
        return Map.copyOf(normalized);
    }

    private Map<String, Object> readPageableJson(String paramCode, String rawValue) {
        try {
            return objectMapper.readValue(rawValue, MAP_TYPE);
        } catch (IOException exception) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "PAGEABLE parameter is not a valid JSON object: " + paramCode,
                    exception
            );
        }
    }

    private int toPositiveInt(String fieldName, Object rawValue) {
        try {
            int normalized = Integer.parseInt(String.valueOf(rawValue).trim());
            if (normalized < 1) {
                throw new DataServicesException(
                        DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                        fieldName + " must be greater than 0"
                );
            }
            return normalized;
        } catch (NumberFormatException exception) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    fieldName + " must be a valid integer",
                    exception
            );
        }
    }

    private void validateAllowedValues(String paramCode, String value, List<String> allowedValues) {
        if (allowedValues == null || allowedValues.isEmpty()) {
            return;
        }
        if (!allowedValues.contains(value)) {
            throw new DataServicesException(
                    DataServicesErrorCode.DATA_COMMON_VALIDATION_ERROR,
                    "Parameter value is not in allowedValues: " + paramCode
            );
        }
    }

    private boolean isIsoDateLike(String value) {
        try {
            Instant.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ignored) {
            return false;
        }
    }
}
