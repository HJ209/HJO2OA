package com.hjo2oa.data.connector.domain;

public record ConnectorParameterView(
        String paramKey,
        String paramValueRef,
        boolean sensitive
) {
}
