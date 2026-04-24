package com.hjo2oa.data.data.sync.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hjo2oa.data.common.domain.event.DataEventTypes;
import com.hjo2oa.data.data.sync.DataSyncTestSupport;
import com.hjo2oa.data.data.sync.application.SyncExecutionApplicationService;
import com.hjo2oa.data.data.sync.application.SyncTaskApplicationService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DataSyncEventListenerTest {

    private SyncTaskApplicationService taskApplicationService;
    private SyncExecutionApplicationService executionApplicationService;
    private DataSyncEventListener dataSyncEventListener;

    @BeforeEach
    void setUp() {
        taskApplicationService = mock(SyncTaskApplicationService.class);
        executionApplicationService = mock(SyncExecutionApplicationService.class);
        dataSyncEventListener = new DataSyncEventListener(
                taskApplicationService,
                executionApplicationService,
                new DomainEventPayloadExtractor(new ObjectMapper())
        );
    }

    @Test
    void shouldRefreshConnectorDependencyWhenConnectorUpdatedEventArrives() {
        UUID connectorId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        DataSyncTestSupport.TestDataEvent event = new DataSyncTestSupport.TestDataEvent(
                UUID.fromString("66666666-6666-6666-6666-666666666666"),
                Instant.parse("2026-04-24T05:00:00Z"),
                DataEventTypes.DATA_CONNECTOR_UPDATED,
                Map.of("connectorId", connectorId.toString(), "status", "DISABLED")
        );

        dataSyncEventListener.onDomainEvent(event);

        verify(taskApplicationService).refreshConnectorDependency(connectorId, "DISABLED");
        verifyNoInteractions(executionApplicationService);
    }

    @Test
    void shouldForwardBusinessEventToExecutionService() {
        DataSyncTestSupport.TestDataEvent event = new DataSyncTestSupport.TestDataEvent(
                UUID.fromString("77777777-7777-7777-7777-777777777777"),
                Instant.parse("2026-04-24T05:05:00Z"),
                "org.person.updated",
                Map.of("personId", "person-1")
        );
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        dataSyncEventListener.onDomainEvent(event);

        verify(executionApplicationService).onBusinessEvent(eq(event), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue()).containsEntry("personId", "person-1");
    }

    @Test
    void shouldForwardSchedulerEventWithEventMetadata() {
        DataSyncTestSupport.TestDataEvent event = new DataSyncTestSupport.TestDataEvent(
                UUID.fromString("88888888-8888-8888-8888-888888888888"),
                Instant.parse("2026-04-24T05:10:00Z"),
                "infra.scheduler.fired",
                Map.of("jobCode", "sync.job.1", "triggerAt", "2026-04-24T05:10:00Z")
        );
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        dataSyncEventListener.onDomainEvent(event);

        verify(executionApplicationService).onSchedulerTrigger(eq("sync.job.1"), payloadCaptor.capture());
        assertThat(payloadCaptor.getValue())
                .containsEntry("jobCode", "sync.job.1")
                .containsEntry("eventId", "88888888-8888-8888-8888-888888888888")
                .containsEntry("occurredAt", "2026-04-24T05:10:00Z");
    }

    @Test
    void shouldIgnoreDataSyncSelfEvents() {
        DataSyncTestSupport.TestDataEvent event = new DataSyncTestSupport.TestDataEvent(
                UUID.fromString("99999999-9999-9999-9999-999999999999"),
                Instant.parse("2026-04-24T05:15:00Z"),
                DataEventTypes.DATA_SYNC_COMPLETED,
                Map.of("taskId", UUID.randomUUID().toString())
        );

        dataSyncEventListener.onDomainEvent(event);

        verifyNoInteractions(taskApplicationService, executionApplicationService);
    }
}
