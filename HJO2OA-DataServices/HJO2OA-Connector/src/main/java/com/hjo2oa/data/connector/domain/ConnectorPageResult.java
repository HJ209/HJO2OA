package com.hjo2oa.data.connector.domain;

import java.util.List;

public record ConnectorPageResult(
        List<ConnectorDefinition> items,
        long total
) {

    public ConnectorPageResult {
        items = List.copyOf(items);
    }
}
