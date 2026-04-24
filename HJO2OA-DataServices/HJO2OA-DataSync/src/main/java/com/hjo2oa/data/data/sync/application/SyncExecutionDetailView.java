package com.hjo2oa.data.data.sync.application;

import com.hjo2oa.data.data.sync.domain.SyncDifferenceItem;
import java.util.List;
import java.util.Map;

public record SyncExecutionDetailView(
        SyncExecutionSummaryView summary,
        List<SyncDifferenceItem> differences,
        Map<String, Object> triggerContext,
        String operatorAccountId,
        String operatorPersonId
) {
}
