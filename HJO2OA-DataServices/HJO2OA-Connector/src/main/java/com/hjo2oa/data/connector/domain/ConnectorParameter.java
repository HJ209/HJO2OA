package com.hjo2oa.data.connector.domain;

import java.util.Objects;

public record ConnectorParameter(
        String parameterId,
        String connectorId,
        String paramKey,
        String paramValueRef,
        boolean sensitive
) {

    private static final String KEY_REF_PREFIX = "keyRef:";

    public ConnectorParameter {
        parameterId = requireText(parameterId, "parameterId");
        connectorId = requireText(connectorId, "connectorId");
        paramKey = requireText(paramKey, "paramKey");
        paramValueRef = requireText(paramValueRef, "paramValueRef");
        if (sensitive && !isKeyReference(paramValueRef)) {
            throw new IllegalArgumentException("Sensitive connector parameters must use a key reference");
        }
    }

    public static ConnectorParameter of(
            String parameterId,
            String connectorId,
            String paramKey,
            String paramValueRef,
            boolean sensitive
    ) {
        return new ConnectorParameter(parameterId, connectorId, paramKey, paramValueRef, sensitive);
    }

    public static boolean isKeyReference(String value) {
        return value != null && value.trim().startsWith(KEY_REF_PREFIX);
    }

    public String keyRefName() {
        if (!isKeyReference(paramValueRef)) {
            return null;
        }
        return paramValueRef.substring(KEY_REF_PREFIX.length()).trim();
    }

    public ConnectorParameterView toView() {
        return new ConnectorParameterView(paramKey, paramValueRef, sensitive);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
