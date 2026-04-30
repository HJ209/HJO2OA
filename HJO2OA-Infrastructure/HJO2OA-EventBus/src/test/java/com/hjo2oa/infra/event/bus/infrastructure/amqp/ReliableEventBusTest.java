package com.hjo2oa.infra.event.bus.infrastructure.amqp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hjo2oa.infra.event.bus.application.EventBusManagementApplicationService;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.EventOperationCommand;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.OperatorContext;
import com.hjo2oa.infra.event.bus.application.EventBusManagementCommands.ReplayEventsCommand;
import com.hjo2oa.infra.event.bus.application.EventBusOperationAudit;
import com.hjo2oa.infra.event.bus.application.EventBusOperationAuditRepository;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;

class ReliableEventBusTest {

    private static final Instant NOW = Instant.parse("2026-04-29T01:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void amqpPublisherPersistsEnvelopeIntoOutbox() throws Exception {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        AmqpDomainEventPublisher publisher = new AmqpDomainEventPublisher(repository, objectMapper, CLOCK);

        publisher.publish(new TestDomainEvent(
                EVENT_ID,
                "workflow.task.created",
                NOW.minusSeconds(5),
                "tenant-1",
                "trace-1",
                "task-1"
        ));

        EventOutboxEntity saved = repository.events().get(0);
        DomainEventEnvelope envelope = objectMapper.readValue(saved.getPayloadJson(), DomainEventEnvelope.class);
        assertThat(saved.getStatus()).isEqualTo(EventOutboxStatus.PENDING.name());
        assertThat(saved.getEventId()).isEqualTo(EVENT_ID);
        assertThat(saved.getAggregateType()).isEqualTo("workflow.task");
        assertThat(saved.getAggregateId()).isEqualTo("task-1");
        assertThat(saved.getTenantId()).isEqualTo("tenant-1");
        assertThat(saved.getTraceId()).isEqualTo("trace-1");
        assertThat(saved.getSchemaVersion()).isEqualTo("1");
        assertThat(envelope.payload().get("taskId").asText()).isEqualTo("task-1");
    }

    @Test
    void outboxPublisherMarksEventPublishedAfterRabbitSendSucceeds() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        EventOutboxEntity event = outboxEvent(EventOutboxStatus.PENDING, 0);
        repository.save(event);
        CapturingRabbitTemplate rabbitTemplate = new CapturingRabbitTemplate(false);
        EventOutboxPublisher publisher = new EventOutboxPublisher(
                repository,
                rabbitTemplate,
                retryPolicy(5),
                CLOCK,
                "exchange",
                50
        );

        publisher.publishPendingEvents();

        EventOutboxEntity updated = repository.events().get(0);
        assertThat(updated.getStatus()).isEqualTo(EventOutboxStatus.PUBLISHED.name());
        assertThat(updated.getPublishedAt()).isEqualTo(NOW);
        assertThat(rabbitTemplate.messageProperties().getMessageId()).isEqualTo(EVENT_ID.toString());
        assertThat((Object) rabbitTemplate.messageProperties().getHeader("eventType"))
                .isEqualTo("workflow.task.created");
    }

    @Test
    void outboxPublisherRetriesWithBackoffAndMovesToDeadAfterMaxRetries() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        repository.save(outboxEvent(EventOutboxStatus.PENDING, 0));
        EventOutboxPublisher publisher = new EventOutboxPublisher(
                repository,
                new CapturingRabbitTemplate(true),
                retryPolicy(2),
                CLOCK,
                "exchange",
                50
        );

        publisher.publishPendingEvents();

        EventOutboxEntity firstFailure = repository.events().get(0);
        assertThat(firstFailure.getStatus()).isEqualTo(EventOutboxStatus.FAILED.name());
        assertThat(firstFailure.getRetryCount()).isEqualTo(1);
        assertThat(firstFailure.getNextRetryAt()).isEqualTo(NOW.plusSeconds(1));

        firstFailure.setNextRetryAt(NOW);
        publisher.publishPendingEvents();

        EventOutboxEntity dead = repository.events().get(0);
        assertThat(dead.getStatus()).isEqualTo(EventOutboxStatus.DEAD.name());
        assertThat(dead.getRetryCount()).isEqualTo(2);
        assertThat(dead.getDeadAt()).isEqualTo(NOW);
        assertThat(dead.getLastError()).contains("send failed");
    }

    @Test
    void managementReplayResetsMatchedEventsAndRecordsAudit() {
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        outboxRepository.save(outboxEvent(EventOutboxStatus.DEAD, 5));
        EventBusManagementApplicationService service = new EventBusManagementApplicationService(
                outboxRepository,
                auditRepository,
                objectMapper,
                CLOCK
        );

        var result = service.replay(new ReplayEventsCommand(
                new EventOutboxQuery(null, "workflow.task.created", null, null, null, null,
                        EventOutboxStatus.DEAD, null, null, 1, 20),
                "operator fixed downstream projection",
                new OperatorContext(null, null, "req-1", "idem-1")
        ));

        assertThat(result.replayedCount()).isEqualTo(1);
        assertThat(outboxRepository.events().get(0).getStatus()).isEqualTo(EventOutboxStatus.PENDING.name());
        assertThat(outboxRepository.events().get(0).getRetryCount()).isZero();
        assertThat(auditRepository.audits()).hasSize(1);
        assertThat(auditRepository.audits().get(0).operationType()).isEqualTo("REPLAY");
    }

    @Test
    void managementRetryAndDeadLetterRecordAuditedStateTransitions() {
        InMemoryOutboxRepository outboxRepository = new InMemoryOutboxRepository();
        InMemoryAuditRepository auditRepository = new InMemoryAuditRepository();
        outboxRepository.save(outboxEvent(EventOutboxStatus.FAILED, 1));
        EventBusManagementApplicationService service = new EventBusManagementApplicationService(
                outboxRepository,
                auditRepository,
                objectMapper,
                CLOCK
        );
        OperatorContext retryOperator = new OperatorContext(null, null, "req-2", "idem-2");
        OperatorContext deadLetterOperator = new OperatorContext(null, null, "req-3", "idem-3");

        service.retry(new EventOperationCommand(EVENT_ID, "retry after broker recovery", retryOperator));
        assertThat(outboxRepository.events().get(0).getStatus()).isEqualTo(EventOutboxStatus.PENDING.name());

        service.deadLetter(new EventOperationCommand(EVENT_ID, "poison event confirmed", deadLetterOperator));
        assertThat(outboxRepository.events().get(0).getStatus()).isEqualTo(EventOutboxStatus.DEAD.name());
        assertThat(auditRepository.audits()).extracting(EventBusOperationAudit::operationType)
                .containsExactly("RETRY", "DEAD_LETTER");
    }

    @Test
    void consumerSkipsDuplicateEventAfterSuccessfulConsumption() throws Exception {
        DomainEventEnvelope envelope = new DomainEventEnvelopeFactory(objectMapper, CLOCK)
                .from(new TestDomainEvent(EVENT_ID, "workflow.task.created", NOW, "tenant-1", "trace-1", "task-1"));
        String body = objectMapper.writeValueAsString(envelope);
        InMemoryConsumedEventRepository consumedRepository = new InMemoryConsumedEventRepository();
        List<Object> publishedEvents = new ArrayList<>();
        ApplicationEventPublisher springPublisher = publishedEvents::add;
        AmqpDomainEventConsumer consumer = new AmqpDomainEventConsumer(
                springPublisher,
                objectMapper,
                consumedRepository,
                CLOCK,
                "consumer-1"
        );

        consumer.onMessage(body);
        consumer.onMessage(body);

        assertThat(publishedEvents).hasSize(1);
        assertThat(consumedRepository.status(EVENT_ID, "consumer-1")).isEqualTo(ConsumedEventStatus.SUCCESS);
    }

    private EventOutboxEntity outboxEvent(EventOutboxStatus status, int retryCount) {
        return new EventOutboxEntity()
                .setId(UUID.randomUUID())
                .setEventId(EVENT_ID)
                .setAggregateType("workflow.task")
                .setAggregateId("task-1")
                .setEventType("workflow.task.created")
                .setTenantId("tenant-1")
                .setOccurredAt(NOW.minusSeconds(5))
                .setTraceId("trace-1")
                .setSchemaVersion("1")
                .setPayloadJson("{\"eventId\":\"" + EVENT_ID + "\"}")
                .setStatus(status.name())
                .setCreatedAt(NOW.minusSeconds(10))
                .setRetryCount(retryCount)
                .setLastError(status == EventOutboxStatus.DEAD ? "dead" : null);
    }

    private EventOutboxRetryPolicy retryPolicy(int maxRetries) {
        EventOutboxRetryProperties properties = new EventOutboxRetryProperties();
        properties.setMaxRetries(maxRetries);
        properties.setInitialIntervalSeconds(1);
        properties.setMultiplier(2);
        properties.setMaxIntervalSeconds(60);
        return new EventOutboxRetryPolicy(properties);
    }

    private record TestDomainEvent(
            UUID eventId,
            String eventType,
            Instant occurredAt,
            String tenantId,
            String traceId,
            String taskId
    ) implements DomainEvent {
    }

    private static final class CapturingRabbitTemplate extends RabbitTemplate {

        private final boolean fail;
        private MessageProperties messageProperties;

        private CapturingRabbitTemplate(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void convertAndSend(
                String exchange,
                String routingKey,
                Object object,
                MessagePostProcessor messagePostProcessor
        ) throws AmqpException {
            if (fail) {
                throw new AmqpException("send failed");
            }
            Message message = new Message(String.valueOf(object).getBytes(StandardCharsets.UTF_8),
                    new MessageProperties());
            Message processed = messagePostProcessor.postProcessMessage(message);
            this.messageProperties = processed.getMessageProperties();
        }

        private MessageProperties messageProperties() {
            return messageProperties;
        }
    }

    private static final class InMemoryOutboxRepository implements EventOutboxRepository {

        private final Map<UUID, EventOutboxEntity> events = new LinkedHashMap<>();

        @Override
        public void save(EventOutboxEntity entity) {
            events.put(entity.getId(), entity);
        }

        @Override
        public List<EventOutboxEntity> findDueForPublish(Instant now, int limit) {
            return events.values().stream()
                    .filter(event -> EventOutboxStatus.PENDING.name().equals(event.getStatus())
                            || (EventOutboxStatus.FAILED.name().equals(event.getStatus())
                            && (event.getNextRetryAt() == null || !event.getNextRetryAt().isAfter(now))))
                    .limit(limit)
                    .toList();
        }

        @Override
        public Optional<EventOutboxEntity> findByEventId(UUID eventId) {
            return events.values().stream()
                    .filter(event -> eventId.equals(event.getEventId()))
                    .findFirst();
        }

        @Override
        public EventOutboxPage query(EventOutboxQuery query) {
            List<EventOutboxEntity> matched = events.values().stream()
                    .filter(event -> query.eventId() == null || query.eventId().equals(event.getEventId()))
                    .filter(event -> query.eventType() == null || query.eventType().equals(event.getEventType()))
                    .filter(event -> query.status() == null || query.status().name().equals(event.getStatus()))
                    .sorted(Comparator.comparing(EventOutboxEntity::getCreatedAt).reversed())
                    .toList();
            return new EventOutboxPage(matched, matched.size());
        }

        @Override
        public EventOutboxStatistics statistics() {
            long pending = count(EventOutboxStatus.PENDING);
            long published = count(EventOutboxStatus.PUBLISHED);
            long failed = count(EventOutboxStatus.FAILED);
            long dead = count(EventOutboxStatus.DEAD);
            return new EventOutboxStatistics(pending, published, failed, dead, pending + published + failed + dead);
        }

        @Override
        public void markPublished(UUID id, Instant publishedAt) {
            events.get(id)
                    .setStatus(EventOutboxStatus.PUBLISHED.name())
                    .setPublishedAt(publishedAt)
                    .setLastError(null)
                    .setNextRetryAt(null)
                    .setDeadAt(null);
        }

        @Override
        public void markFailed(UUID id, int retryCount, Instant nextRetryAt, String lastError) {
            events.get(id)
                    .setStatus(EventOutboxStatus.FAILED.name())
                    .setRetryCount(retryCount)
                    .setNextRetryAt(nextRetryAt)
                    .setLastError(lastError);
        }

        @Override
        public void markDead(UUID id, int retryCount, String lastError, Instant deadAt) {
            events.get(id)
                    .setStatus(EventOutboxStatus.DEAD.name())
                    .setRetryCount(retryCount)
                    .setLastError(lastError)
                    .setDeadAt(deadAt)
                    .setNextRetryAt(null);
        }

        @Override
        public void resetForReplay(UUID id, Instant now) {
            events.get(id)
                    .setStatus(EventOutboxStatus.PENDING.name())
                    .setRetryCount(0)
                    .setNextRetryAt(now)
                    .setPublishedAt(null)
                    .setLastError(null)
                    .setDeadAt(null);
        }

        private List<EventOutboxEntity> events() {
            return new ArrayList<>(events.values());
        }

        private long count(EventOutboxStatus status) {
            return events.values().stream()
                    .filter(event -> status.name().equals(event.getStatus()))
                    .count();
        }
    }

    private static final class InMemoryAuditRepository implements EventBusOperationAuditRepository {

        private final List<EventBusOperationAudit> audits = new ArrayList<>();

        @Override
        public EventBusOperationAudit save(EventBusOperationAudit audit) {
            if (findByIdempotencyKey(audit.idempotencyKey()).isEmpty()) {
                audits.add(audit);
            }
            return audit;
        }

        @Override
        public Optional<EventBusOperationAudit> findByIdempotencyKey(String idempotencyKey) {
            if (idempotencyKey == null) {
                return Optional.empty();
            }
            return audits.stream()
                    .filter(audit -> idempotencyKey.equals(audit.idempotencyKey()))
                    .findFirst();
        }

        private List<EventBusOperationAudit> audits() {
            return audits;
        }
    }

    private static final class InMemoryConsumedEventRepository implements ConsumedEventRepository {

        private final Map<String, ConsumedEventStatus> statuses = new LinkedHashMap<>();

        @Override
        public boolean tryStart(DomainEventEnvelope envelope, String consumerCode, Instant now) {
            String key = key(envelope.eventId(), consumerCode);
            ConsumedEventStatus status = statuses.get(key);
            if (status == null || status == ConsumedEventStatus.FAILED) {
                statuses.put(key, ConsumedEventStatus.PROCESSING);
                return true;
            }
            return false;
        }

        @Override
        public void markSuccess(UUID eventId, String consumerCode, Instant now) {
            statuses.put(key(eventId, consumerCode), ConsumedEventStatus.SUCCESS);
        }

        @Override
        public void markFailed(UUID eventId, String consumerCode, String lastError, Instant now) {
            statuses.put(key(eventId, consumerCode), ConsumedEventStatus.FAILED);
        }

        private ConsumedEventStatus status(UUID eventId, String consumerCode) {
            return statuses.get(key(eventId, consumerCode));
        }

        private String key(UUID eventId, String consumerCode) {
            return eventId + "::" + consumerCode;
        }
    }
}
