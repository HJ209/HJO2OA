package com.hjo2oa.infra.event.bus.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class EventBusManagementViews {

    private EventBusManagementViews() {
    }

    public record EventSummaryView(
            UUID id,
            UUID eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String tenantId,
            Instant occurredAt,
            String traceId,
            String schemaVersion,
            String status,
            int retryCount,
            Instant nextRetryAt,
            Instant publishedAt,
            Instant deadAt,
            String lastError,
            Instant createdAt
    ) {
    }

    public record EventDetailView(
            UUID id,
            UUID eventId,
            String eventType,
            String aggregateType,
            String aggregateId,
            String tenantId,
            Instant occurredAt,
            String traceId,
            String schemaVersion,
            String status,
            int retryCount,
            Instant nextRetryAt,
            Instant publishedAt,
            Instant deadAt,
            String lastError,
            String payloadJson,
            Instant createdAt
    ) {
    }

    public record EventStatisticsView(
            long pending,
            long published,
            long failed,
            long dead,
            long total
    ) {
    }

    public record ReplayResultView(
            int replayedCount,
            List<UUID> eventIds,
            Instant replayedAt
    ) {

        public ReplayResultView {
            eventIds = List.copyOf(eventIds);
        }
    }
}
