package com.hjo2oa.bootstrap.dataservices;

import static org.assertj.core.api.Assertions.assertThatNoException;

import com.hjo2oa.bootstrap.BootstrapContextTestConfiguration;
import com.hjo2oa.bootstrap.Hjo2oaApplication;
import com.hjo2oa.data.common.domain.event.AbstractDataDomainEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataApiPublishedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataConnectorUpdatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataReportRefreshedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataServiceActivatedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncCompletedEvent;
import com.hjo2oa.data.governance.domain.GovernanceContractEvents.DataSyncFailedEvent;
import com.hjo2oa.data.governance.domain.GovernanceProfileRepository;
import com.hjo2oa.data.governance.domain.GovernanceRuntimeRepository;
import com.hjo2oa.data.governance.domain.GovernanceTypes.RuntimeTargetStatus;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {Hjo2oaApplication.class, BootstrapContextTestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.lazy-initialization=true",
                "logging.level.com.hjo2oa=INFO",
                "hjo2oa.cache.type=inmemory",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
                        + "org.flowable.spring.boot.ProcessEngineAutoConfiguration,"
                        + "org.flowable.spring.boot.FlowableAutoConfiguration,"
                        + "org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration,"
                        + "org.flowable.spring.boot.idm.IdmAutoConfiguration,"
                        + "org.flowable.spring.boot.app.AppAutoConfiguration"
        }
)
@ActiveProfiles("local")
class DataServicesEventChainTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @MockBean
    private GovernanceProfileRepository governanceProfileRepository;

    @MockBean
    private GovernanceRuntimeRepository governanceRuntimeRepository;

    @Test
    @DisplayName("DataSync listener should handle connector updated event without error")
    void shouldHandleConnectorUpdatedEventInDataSyncListener() {
        DomainEvent event = new TestDomainEvent(
                "data.connector.updated",
                "tenant-1",
                "connector",
                UUID.randomUUID().toString(),
                "operator-1",
                Map.of("connectorId", UUID.randomUUID().toString(), "status", "ACTIVE")
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("DataSync listener should handle scheduler trigger event without error")
    void shouldHandleSchedulerTriggerEventInDataSyncListener() {
        DomainEvent event = new TestDomainEvent(
                "infra.scheduler.sync-trigger",
                "tenant-1",
                "sync",
                UUID.randomUUID().toString(),
                "system",
                Map.of("jobCode", "daily-sync")
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("Governance listener should handle API published event without error")
    void shouldHandleApiPublishedEventInGovernanceListener() {
        DataApiPublishedEvent event = new DataApiPublishedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "tenant-1",
                "api-1",
                "queryUser",
                "v1",
                "/api/users",
                "GET",
                RuntimeTargetStatus.ACTIVE
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("Governance listener should handle connector updated event without error")
    void shouldHandleConnectorUpdatedEventInGovernanceListener() {
        UUID connectorUuid = UUID.randomUUID();
        DataConnectorUpdatedEvent event = new DataConnectorUpdatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "tenant-1",
                connectorUuid.toString(),
                "erp-connector",
                "REST",
                RuntimeTargetStatus.ACTIVE
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("Governance listener should handle sync completed event without error")
    void shouldHandleSyncCompletedEventInGovernanceListener() {
        DataSyncCompletedEvent event = new DataSyncCompletedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "tenant-1",
                "task-1",
                "daily-sync",
                "exec-1",
                100L,
                50L,
                0L
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("Governance listener should handle sync failed event without error")
    void shouldHandleSyncFailedEventInGovernanceListener() {
        DataSyncFailedEvent event = new DataSyncFailedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "tenant-1",
                "task-1",
                "daily-sync",
                "exec-1",
                "TIMEOUT",
                "Connection timed out",
                true
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("Governance listener should handle report refreshed event without error")
    void shouldHandleReportRefreshedEventInGovernanceListener() {
        DataReportRefreshedEvent event = new DataReportRefreshedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "tenant-1",
                "report-1",
                "daily-stats",
                Instant.now(),
                "FRESH"
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    @Test
    @DisplayName("Governance listener should handle service activated event without error")
    void shouldHandleServiceActivatedEventInGovernanceListener() {
        DataServiceActivatedEvent event = new DataServiceActivatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "tenant-1",
                "service-1",
                "queryUser",
                "QUERY",
                "PUBLIC"
        );
        assertThatNoException().isThrownBy(() -> eventPublisher.publishEvent(event));
    }

    private static class TestDomainEvent extends AbstractDataDomainEvent {

        TestDomainEvent(
                String eventType,
                String tenantId,
                String moduleCode,
                String aggregateCode,
                String operatorId,
                Map<String, Object> payload
        ) {
            super(eventType, tenantId, moduleCode, aggregateCode, operatorId, payload);
        }
    }
}
