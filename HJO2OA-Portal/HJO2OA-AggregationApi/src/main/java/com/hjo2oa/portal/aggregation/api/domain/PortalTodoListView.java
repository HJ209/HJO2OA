package com.hjo2oa.portal.aggregation.api.domain;

import com.hjo2oa.shared.web.PageData;
import java.time.Instant;

public record PortalTodoListView(
        PortalTodoListViewType viewType,
        PortalTodoListSummary summary,
        PageData<PortalTodoListItem> todos,
        Instant generatedAt
) {
}
