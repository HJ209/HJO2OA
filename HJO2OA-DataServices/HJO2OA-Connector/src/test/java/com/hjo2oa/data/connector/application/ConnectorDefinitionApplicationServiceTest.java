package com.hjo2oa.data.connector.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.data.connector.domain.ConnectorAuthMode;
import com.hjo2oa.data.connector.domain.ConnectorCheckType;
import com.hjo2oa.data.connector.domain.ConnectorContext;
import com.hjo2oa.data.connector.domain.ConnectorDefinitionView;
import com.hjo2oa.data.connector.domain.ConnectorHealthOverviewView;
import com.hjo2oa.data.connector.domain.ConnectorHealthSnapshotView;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplate;
import com.hjo2oa.data.connector.domain.ConnectorParameterTemplateCategory;
import com.hjo2oa.data.connector.domain.ConnectorStatus;
import com.hjo2oa.data.connector.domain.ConnectorType;
import com.hjo2oa.data.connector.domain.DataConnectorUpdatedEvent;
import com.hjo2oa.data.connector.domain.TimeoutRetryConfig;
import com.hjo2oa.data.connector.infrastructure.ConnectorDriverRegistry;
import com.hjo2oa.data.connector.infrastructure.ConnectorSecretValueResolver;
import com.hjo2oa.data.connector.infrastructure.DatabaseConnectorDriver;
import com.hjo2oa.data.connector.infrastructure.DriverManagerJdbcConnectivityClient;
import com.hjo2oa.data.connector.infrastructure.HttpConnectorDriver;
import com.hjo2oa.data.connector.infrastructure.HttpConnectivityClient;
import com.hjo2oa.data.connector.infrastructure.HttpRequestSpec;
import com.hjo2oa.data.connector.infrastructure.InMemoryConnectorDefinitionRepository;
import com.hjo2oa.data.connector.infrastructure.MessageQueueConnectionSpec;
import com.hjo2oa.data.connector.infrastructure.MessageQueueConnectorDriver;
import com.hjo2oa.data.connector.infrastructure.MessageQueueConnectivityClient;
import com.hjo2oa.data.connector.infrastructure.StaticConnectorContextProvider;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConnectorDefinitionApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T02:00:00Z");

    @Test
    void shouldCreateConfigureTestAndActivateConnector() {
        InMemoryConnectorDefinitionRepository repository = new InMemoryConnectorDefinitionRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        ConnectorDefinitionApplicationService applicationService = applicationService(
                repository,
                publishedEvents,
                successHttpClient()
        );

        ConnectorDefinitionView created = applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-1",
                "http-demo",
                "HTTP Demo",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.TOKEN,
                new TimeoutRetryConfig(2_000, 3_000, 1, 10)
        ));
        ConnectorDefinitionView configured = applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-1",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("healthPath", "/health", false),
                        new ConnectorParameterValue("token", "keyRef:http.demo.token", true)
                )
        ));
        ConnectorHealthSnapshotView healthSnapshot = applicationService.testConnection("connector-http-1");
        ConnectorHealthSnapshotView refreshedHealth = applicationService.refreshHealth("connector-http-1");
        ConnectorHealthOverviewView healthOverview = applicationService.latestHealthOverview("connector-http-1").orElseThrow();
        ConnectorDefinitionView activated = applicationService.activate("connector-http-1");

        assertThat(created.status()).isEqualTo(ConnectorStatus.DRAFT);
        assertThat(configured.parameters()).hasSize(3);
        assertThat(healthSnapshot.checkType()).isEqualTo(ConnectorCheckType.MANUAL_TEST);
        assertThat(healthSnapshot.healthStatus().name()).isEqualTo("HEALTHY");
        assertThat(healthSnapshot.targetEnvironment()).isEqualTo("local");
        assertThat(refreshedHealth.checkType()).isEqualTo(ConnectorCheckType.HEALTH_CHECK);
        assertThat(healthOverview.latestHealthSnapshot().checkType()).isEqualTo(ConnectorCheckType.HEALTH_CHECK);
        assertThat(healthOverview.healthyCount()).isEqualTo(1);
        assertThat(activated.status()).isEqualTo(ConnectorStatus.ACTIVE);
        assertThat(activated.latestTestSnapshot()).isNotNull();
        assertThat(activated.latestHealthSnapshot()).isNotNull();
        assertThat(publishedEvents)
                .filteredOn(DataConnectorUpdatedEvent.class::isInstance)
                .hasSize(3);
        DataConnectorUpdatedEvent latestEvent = (DataConnectorUpdatedEvent) publishedEvents.get(2);
        assertThat(latestEvent.eventType()).isEqualTo("data.connector.updated");
        assertThat(latestEvent.changedFields()).contains("status");
    }

    @Test
    void shouldDowngradeActiveConnectorToDraftAfterParameterChange() {
        ConnectorDefinitionApplicationService applicationService = applicationService(
                new InMemoryConnectorDefinitionRepository(),
                new ArrayList<>(),
                successHttpClient()
        );
        applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-2",
                "http-draft-demo",
                "HTTP Draft Demo",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.TOKEN,
                TimeoutRetryConfig.defaultConfig()
        ));
        applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-2",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("token", "keyRef:http.demo.token", true)
                )
        ));
        applicationService.testConnection("connector-http-2");
        applicationService.activate("connector-http-2");

        ConnectorDefinitionView updated = applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-2",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("token", "keyRef:http.demo.token.v2", true)
                )
        ));

        assertThat(updated.status()).isEqualTo(ConnectorStatus.DRAFT);
        assertThatThrownBy(() -> applicationService.activate("connector-http-2"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("最新配置");
    }

    @Test
    void shouldRequireLatestManualTestInsteadOfHealthRefreshForActivation() {
        ConnectorDefinitionApplicationService applicationService = applicationService(
                new InMemoryConnectorDefinitionRepository(),
                new ArrayList<>(),
                successHttpClient()
        );
        applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-5",
                "http-health-only",
                "HTTP Health Only",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.TOKEN,
                TimeoutRetryConfig.defaultConfig()
        ));
        applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-5",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("token", "keyRef:http.demo.token", true)
                )
        ));
        applicationService.refreshHealth("connector-http-5");

        assertThatThrownBy(() -> applicationService.activate("connector-http-5"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("连接测试");
    }

    @Test
    void shouldDeduplicateManualTestWithinConnectivityCheckWindow() {
        InMemoryConnectorDefinitionRepository repository = new InMemoryConnectorDefinitionRepository();
        AtomicInteger attempts = new AtomicInteger();
        ConnectorDefinitionApplicationService applicationService = applicationService(
                repository,
                new ArrayList<>(),
                requestSpec -> {
                    attempts.incrementAndGet();
                    return 200;
                }
        );
        applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-6",
                "http-dedup-demo",
                "HTTP Dedup Demo",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.TOKEN,
                TimeoutRetryConfig.defaultConfig()
        ));
        applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-6",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("token", "keyRef:http.demo.token", true)
                )
        ));

        ConnectorHealthSnapshotView first = applicationService.testConnection("connector-http-6");
        ConnectorHealthSnapshotView second = applicationService.testConnection("connector-http-6");

        assertThat(first.snapshotId()).isEqualTo(second.snapshotId());
        assertThat(attempts).hasValue(1);
    }

    @Test
    void shouldConfirmUnhealthyHealthSnapshot() {
        ConnectorDefinitionApplicationService applicationService = applicationService(
                new InMemoryConnectorDefinitionRepository(),
                new ArrayList<>(),
                requestSpec -> {
                    throw new IllegalStateException("down");
                }
        );
        applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-7",
                "http-confirm-demo",
                "HTTP Confirm Demo",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.TOKEN,
                TimeoutRetryConfig.defaultConfig()
        ));
        applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-7",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("token", "keyRef:http.demo.token", true)
                )
        ));

        ConnectorHealthSnapshotView unhealthy = applicationService.refreshHealth("connector-http-7");
        ConnectorHealthSnapshotView confirmed = applicationService.confirmHealthAbnormal(
                "connector-http-7",
                unhealthy.snapshotId(),
                "已人工确认网络异常"
        );

        assertThat(unhealthy.healthStatus()).isNotEqualTo(com.hjo2oa.data.connector.domain.ConnectorHealthStatus.HEALTHY);
        assertThat(confirmed.confirmedBy()).isEqualTo("connector-admin");
        assertThat(confirmed.confirmationNote()).isEqualTo("已人工确认网络异常");
        assertThat(confirmed.confirmedAt()).isNotNull();
    }

    @Test
    void shouldRejectSensitiveParameterWithoutKeyReference() {
        ConnectorDefinitionApplicationService applicationService = applicationService(
                new InMemoryConnectorDefinitionRepository(),
                new ArrayList<>(),
                successHttpClient()
        );
        applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-3",
                "http-secret-demo",
                "HTTP Secret Demo",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.TOKEN,
                TimeoutRetryConfig.defaultConfig()
        ));

        assertThatThrownBy(() -> applicationService.configureParameters(new ConfigureConnectorParametersCommand(
                "connector-http-3",
                List.of(
                        new ConnectorParameterValue("baseUrl", "https://example.test", false),
                        new ConnectorParameterValue("token", "plain-token", true)
                )
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("key reference");
    }

    @Test
    void shouldExposeTemplatesForHttpDatabaseAndMqDrivers() {
        ConnectorDefinitionApplicationService applicationService = applicationService(
                new InMemoryConnectorDefinitionRepository(),
                new ArrayList<>(),
                successHttpClient()
        );
        applicationService.upsert(new UpsertConnectorDefinitionCommand(
                "connector-http-4",
                "http-template-demo",
                "HTTP Template Demo",
                ConnectorType.HTTP,
                "demo",
                "https",
                ConnectorAuthMode.BASIC,
                TimeoutRetryConfig.defaultConfig()
        ));

        List<ConnectorParameterTemplate> templates = applicationService.parameterTemplates(
                "connector-http-4",
                ConnectorParameterTemplateCategory.AUTH,
                true
        );

        assertThat(templates)
                .extracting(ConnectorParameterTemplate::paramKey)
                .contains("password");
    }

    private ConnectorDefinitionApplicationService applicationService(
            InMemoryConnectorDefinitionRepository repository,
            List<DomainEvent> publishedEvents,
            HttpConnectivityClient httpConnectivityClient
    ) {
        ConnectorSecretValueResolver secretValueResolver = (paramValueRef, sensitive) -> {
            if (!sensitive) {
                return paramValueRef;
            }
            if ("keyRef:http.demo.token".equals(paramValueRef)) {
                return "token-1";
            }
            if ("keyRef:http.demo.token.v2".equals(paramValueRef)) {
                return "token-2";
            }
            if ("keyRef:db.password".equals(paramValueRef)) {
                return "db-password";
            }
            if ("keyRef:mq.password".equals(paramValueRef)) {
                return "mq-password";
            }
            throw new IllegalArgumentException("Unexpected test keyRef: " + paramValueRef);
        };
        ConnectorDriverRegistry registry = new ConnectorDriverRegistry(List.of(
                new HttpConnectorDriver(secretValueResolver, httpConnectivityClient),
                new DatabaseConnectorDriver(secretValueResolver, new DriverManagerJdbcConnectivityClient() {
                    @Override
                    public void validate(
                            String jdbcUrl,
                            String username,
                            String password,
                            String validationQuery,
                            TimeoutRetryConfig timeoutRetryConfig
                    ) {
                        // No-op for application-layer unit tests.
                    }
                }),
                new MessageQueueConnectorDriver(secretValueResolver, new MessageQueueConnectivityClient() {
                    @Override
                    public void validate(
                            MessageQueueConnectionSpec connectionSpec,
                            TimeoutRetryConfig timeoutRetryConfig
                    ) {
                        // No-op for application-layer unit tests.
                    }
                })
        ));
        return new ConnectorDefinitionApplicationService(
                repository,
                () -> new ConnectorContext("tenant-1", "connector-admin", "local"),
                registry,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private HttpConnectivityClient successHttpClient() {
        return new HttpConnectivityClient() {
            @Override
            public int execute(HttpRequestSpec requestSpec) {
                return 200;
            }
        };
    }
}
