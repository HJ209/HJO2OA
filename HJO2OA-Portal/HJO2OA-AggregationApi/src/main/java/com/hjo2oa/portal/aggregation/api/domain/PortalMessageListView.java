package com.hjo2oa.portal.aggregation.api.domain;

import com.hjo2oa.shared.web.PageData;
import java.time.Instant;

public record PortalMessageListView(
        PortalMessageUnreadSummary unreadSummary,
        PageData<PortalMessageListItem> messages,
        Instant generatedAt
) {
}
