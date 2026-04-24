package com.hjo2oa.data.connector.domain;

import com.hjo2oa.shared.web.Pagination;
import java.util.List;

public record ConnectorListView(
        List<ConnectorSummaryView> items,
        Pagination pagination,
        ConnectorListFilterView filters
) {

    public ConnectorListView {
        items = List.copyOf(items);
    }
}
