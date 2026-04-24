package com.hjo2oa.data.report.domain;

import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DataReportRefreshedEvent(
        UUID eventId,
        Instant occurredAt,
        String tenantId,
        String reportId,
        String code,
        ReportType reportType,
        Instant snapshotAt,
        ReportFreshnessStatus freshnessStatus
) implements DomainEvent {

    public static final String EVENT_TYPE = "data.report.refreshed";

    public DataReportRefreshedEvent {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        tenantId = requireText(tenantId, "tenantId");
        reportId = requireText(reportId, "reportId");
        code = requireText(code, "code");
        Objects.requireNonNull(reportType, "reportType must not be null");
        Objects.requireNonNull(snapshotAt, "snapshotAt must not be null");
        Objects.requireNonNull(freshnessStatus, "freshnessStatus must not be null");
    }

    public static DataReportRefreshedEvent from(ReportDefinition reportDefinition, ReportSnapshot snapshot) {
        return new DataReportRefreshedEvent(
                UUID.randomUUID(),
                snapshot.snapshotAt(),
                reportDefinition.tenantId(),
                reportDefinition.id(),
                reportDefinition.code(),
                reportDefinition.reportType(),
                snapshot.snapshotAt(),
                snapshot.freshnessStatus()
        );
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
