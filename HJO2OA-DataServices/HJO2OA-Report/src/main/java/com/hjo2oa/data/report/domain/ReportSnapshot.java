package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.Objects;

public record ReportSnapshot(
        String id,
        String reportId,
        Instant snapshotAt,
        String refreshBatch,
        String scopeSignature,
        ReportSnapshotPayload payload,
        ReportFreshnessStatus freshnessStatus,
        ReportRefreshTriggerMode triggerMode,
        String triggerReason,
        String errorMessage
) {

    public static final String GLOBAL_SCOPE = "GLOBAL";

    public ReportSnapshot {
        id = blankToNull(id);
        reportId = requireText(reportId, "reportId");
        Objects.requireNonNull(snapshotAt, "snapshotAt must not be null");
        refreshBatch = requireText(refreshBatch, "refreshBatch");
        scopeSignature = requireText(scopeSignature == null ? GLOBAL_SCOPE : scopeSignature, "scopeSignature");
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(freshnessStatus, "freshnessStatus must not be null");
        Objects.requireNonNull(triggerMode, "triggerMode must not be null");
        triggerReason = blankToNull(triggerReason);
        errorMessage = blankToNull(errorMessage);
    }

    public static ReportSnapshot ready(
            String reportId,
            Instant snapshotAt,
            String refreshBatch,
            ReportSnapshotPayload payload,
            ReportRefreshTriggerMode triggerMode,
            String triggerReason
    ) {
        return new ReportSnapshot(
                null,
                reportId,
                snapshotAt,
                refreshBatch,
                GLOBAL_SCOPE,
                payload,
                ReportFreshnessStatus.READY,
                triggerMode,
                triggerReason,
                null
        );
    }

    public static ReportSnapshot failed(
            String reportId,
            Instant snapshotAt,
            String refreshBatch,
            ReportSnapshotPayload payload,
            ReportRefreshTriggerMode triggerMode,
            String triggerReason,
            String errorMessage
    ) {
        return new ReportSnapshot(
                null,
                reportId,
                snapshotAt,
                refreshBatch,
                GLOBAL_SCOPE,
                payload,
                ReportFreshnessStatus.FAILED,
                triggerMode,
                triggerReason,
                errorMessage
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
