package com.hjo2oa.data.connector.domain;

public record ConnectorListFilterView(
        ConnectorType connectorType,
        ConnectorStatus status,
        String code,
        String keyword,
        int page,
        int size
) {
}
