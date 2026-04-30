package com.hjo2oa.infra.event.bus.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.EventOperationCommand;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.OperatorContext;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.ReplayEventsCommand;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.EventDetailView;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.EventStatisticsView;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.EventSummaryView;
import com.hjo2oa.infra.event.bus.application.EventBusManagementViews.ReplayResultView;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxEntity;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxPage;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxQuery;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxRepository;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxStatistics;
import com.hjo2oa.infra.event.bus.infrastructure.amqp.EventOutboxStatus;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventBusManagementApplicationService {

    private static final int REPLAY_LIMIT = 500;

    private final EventOutboxRepository outboxRepository;
    private final EventBusOperationAuditRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public EventBusManagementApplicationService(
            EventOutboxRepository outboxRepository,
            EventBusOperationAuditRepository auditRepository,
            ObjectMapper objectMapper
    ) {
        this(outboxRepository, auditRepository, objectMapper, Clock.systemUTC());
    }

    public EventBusManagementApplicationService(
            EventOutboxRepository outboxRepository,
            EventBusOperationAuditRepository auditRepository,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.outboxRepository = Objects.requireNonNull(outboxRepository, "outboxRepository must not be null");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public EventOutboxPage listEvents(EventOutboxQuery query) {
        return outboxRepository.query(query);
    }

    public EventDetailView detail(UUID eventId) {
        return toDetail(loadRequired(eventId));
    }

    public EventStatisticsView statistics() {
        EventOutboxStatistics statistics = outboxRepository.statistics();
        return new EventStatisticsView(
                statistics.pending(),
                statistics.published(),
                statistics.failed(),
                statistics.dead(),
                statistics.total()
        );
    }

    public EventDetailView retry(EventOperationCommand command) {
        EventOutboxEntity event = loadRequired(command.eventId());
        EventOutboxStatus status = EventOutboxStatus.valueOf(event.getStatus());
        if (status != EventOutboxStatus.FAILED && status != EventOutboxStatus.DEAD) {
            throw new BizException(
                    EventBusErrorDescriptors.INVALID_OPERATION,
                    "Only FAILED or DEAD events can be manually retried"
            );
        }
        Instant now = clock.instant();
        outboxRepository.resetForReplay(event.getId(), now);
        recordAudit("RETRY", event, command.reason(), command.operatorContext(), detailJson(Map.of(
                "previousStatus", event.getStatus(),
                "eventType", event.getEventType()
        )), now);
        return detail(command.eventId());
    }

    public EventDetailView deadLetter(EventOperationCommand command) {
        EventOutboxEntity event = loadRequired(command.eventId());
        Instant now = clock.instant();
        int retryCount = event.getRetryCount() == null ? 0 : event.getRetryCount();
        outboxRepository.markDead(event.getId(), retryCount, command.reason(), now);
        recordAudit("DEAD_LETTER", event, command.reason(), command.operatorContext(), detailJson(Map.of(
                "previousStatus", event.getStatus(),
                "eventType", event.getEventType()
        )), now);
        return detail(command.eventId());
    }

    public ReplayResultView replay(ReplayEventsCommand command) {
        EventOutboxQuery replayQuery = command.query().withPage(1, REPLAY_LIMIT);
        EventOutboxPage page = outboxRepository.query(replayQuery);
        Instant now = clock.instant();
        List<UUID> replayedEventIds = page.items().stream()
                .peek(event -> {
                    outboxRepository.resetForReplay(event.getId(), now);
                    recordAudit("REPLAY", event, command.reason(), command.operatorContext(),
                            detailJson(command.query()), now);
                })
                .map(EventOutboxEntity::getEventId)
                .toList();
        return new ReplayResultView(replayedEventIds.size(), replayedEventIds, now);
    }

    public EventSummaryView toSummary(EventOutboxEntity entity) {
        return new EventSummaryView(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getTenantId(),
                entity.getOccurredAt(),
                entity.getTraceId(),
                entity.getSchemaVersion(),
                entity.getStatus(),
                entity.getRetryCount() == null ? 0 : entity.getRetryCount(),
                entity.getNextRetryAt(),
                entity.getPublishedAt(),
                entity.getDeadAt(),
                entity.getLastError(),
                entity.getCreatedAt()
        );
    }

    private EventDetailView toDetail(EventOutboxEntity entity) {
        return new EventDetailView(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getTenantId(),
                entity.getOccurredAt(),
                entity.getTraceId(),
                entity.getSchemaVersion(),
                entity.getStatus(),
                entity.getRetryCount() == null ? 0 : entity.getRetryCount(),
                entity.getNextRetryAt(),
                entity.getPublishedAt(),
                entity.getDeadAt(),
                entity.getLastError(),
                entity.getPayloadJson(),
                entity.getCreatedAt()
        );
    }

    private EventOutboxEntity loadRequired(UUID eventId) {
        return outboxRepository.findByEventId(eventId)
                .orElseThrow(() -> new BizException(
                        EventBusErrorDescriptors.EVENT_NOT_FOUND,
                        "Event outbox record not found"
                ));
    }

    private void recordAudit(
            String operationType,
            EventOutboxEntity event,
            String reason,
            OperatorContext operatorContext,
            String detailJson,
            Instant now
    ) {
        String idempotencyKey = operatorContext.idempotencyKey();
        if (idempotencyKey != null && auditRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            return;
        }
        auditRepository.save(new EventBusOperationAudit(
                UUID.randomUUID(),
                event.getEventId(),
                operationType,
                operatorContext.operatorAccountId(),
                operatorContext.operatorPersonId(),
                event.getTenantId(),
                event.getTraceId(),
                operatorContext.requestId(),
                idempotencyKey,
                reason,
                detailJson,
                now
        ));
    }

    private String detailJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize audit detail", ex);
        }
    }
}
