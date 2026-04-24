package com.hjo2oa.data.connector.application;

public record ConnectorParameterValue(
        String paramKey,
        String paramValueRef,
        boolean sensitive
) {
}
