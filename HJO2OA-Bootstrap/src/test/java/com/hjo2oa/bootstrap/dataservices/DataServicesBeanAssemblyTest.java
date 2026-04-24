package com.hjo2oa.bootstrap.dataservices;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.bootstrap.Hjo2oaApplication;
import com.hjo2oa.data.common.audit.InMemoryDataAuditRecorder;
import com.hjo2oa.data.connector.application.ConnectorDefinitionApplicationService;
import com.hjo2oa.data.data.sync.interfaces.DataSyncEventListener;
import com.hjo2oa.data.governance.application.GovernanceMonitoringApplicationService;
import com.hjo2oa.data.governance.interfaces.GovernanceRuntimeEventListener;
import com.hjo2oa.data.openapi.application.OpenApiInvocationApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = Hjo2oaApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.main.lazy-initialization=true",
                "logging.level.com.hjo2oa=INFO",
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
        }
)
@ActiveProfiles("local")
class DataServicesBeanAssemblyTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("Connector application service bean should be present")
    void shouldLoadConnectorApplicationService() {
        assertThat(applicationContext.getBean(ConnectorDefinitionApplicationService.class))
                .isNotNull();
    }

    @Test
    @DisplayName("OpenApi invocation application service bean should be present")
    void shouldLoadOpenApiInvocationApplicationService() {
        assertThat(applicationContext.getBean(OpenApiInvocationApplicationService.class))
                .isNotNull();
    }

    @Test
    @DisplayName("DataSync event listener bean should be present")
    void shouldLoadDataSyncEventListener() {
        assertThat(applicationContext.getBean(DataSyncEventListener.class))
                .isNotNull();
    }

    @Test
    @DisplayName("Governance runtime event listener bean should be present")
    void shouldLoadGovernanceRuntimeEventListener() {
        assertThat(applicationContext.getBean(GovernanceRuntimeEventListener.class))
                .isNotNull();
    }

    @Test
    @DisplayName("Governance monitoring application service bean should be present")
    void shouldLoadGovernanceMonitoringApplicationService() {
        assertThat(applicationContext.getBean(GovernanceMonitoringApplicationService.class))
                .isNotNull();
    }

    @Test
    @DisplayName("In-memory data audit recorder fallback bean should be present")
    void shouldLoadInMemoryDataAuditRecorder() {
        assertThat(applicationContext.getBean(InMemoryDataAuditRecorder.class))
                .isNotNull();
    }
}
