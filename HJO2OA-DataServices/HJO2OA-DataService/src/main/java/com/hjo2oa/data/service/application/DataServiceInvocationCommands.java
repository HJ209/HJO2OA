package com.hjo2oa.data.service.application;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class DataServiceInvocationCommands {

    private DataServiceInvocationCommands() {
    }

    public record InvocationCommand(
            String serviceCode,
            String appCode,
            String subjectId,
            String idempotencyKey,
            Map<String, Object> parameters
    ) {

        public InvocationCommand {
            Objects.requireNonNull(serviceCode, "serviceCode must not be null");
            String normalizedServiceCode = serviceCode.trim();
            if (normalizedServiceCode.isEmpty()) {
                throw new IllegalArgumentException("serviceCode must not be blank");
            }
            serviceCode = normalizedServiceCode;
            appCode = normalizeNullableText(appCode);
            subjectId = normalizeNullableText(subjectId);
            idempotencyKey = normalizeNullableText(idempotencyKey);
            Map<String, Object> copied = new LinkedHashMap<>();
            if (parameters != null && !parameters.isEmpty()) {
                copied.putAll(parameters);
            }
            parameters = Collections.unmodifiableMap(copied);
        }

        private static String normalizeNullableText(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }
    }
}
