package com.hjo2oa.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
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
class Hjo2oaApplicationSmokeTest {

    @Autowired
    private Environment environment;

    @Test
    void shouldLoadBootstrapContextWithLocalProfileDefaults() {
        assertThat(environment.getActiveProfiles()).contains("local");
        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:sqlserver://localhost:1433;databaseName=hjo2oa_dev;encrypt=true;trustServerCertificate=true");
        assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("hjo2oa.storage.endpoint"))
                .isEqualTo("http://localhost:9000");
        assertThat(environment.getProperty("hjo2oa.storage.bucket")).isEqualTo("hjo2oa-local");
    }

    @Test
    void shouldLoadDataServicesConfigurationDefaults() {
        assertThat(environment.getProperty("hjo2oa.data-services.sync.schedule-poller-interval"))
                .isEqualTo("300000");
        assertThat(environment.getProperty("hjo2oa.data-services.report.refresh-cron"))
                .isEqualTo("0 0 * * * *");
        assertThat(environment.getProperty("hjo2oa.data-services.governance.health-check-cron"))
                .isEqualTo("0 0/10 * * * *");
        assertThat(environment.getProperty("hjo2oa.data-services.openapi.auth.signature-algorithm"))
                .isEqualTo("HmacSHA256");
        assertThat(environment.getProperty("hjo2oa.data-services.openapi.quota.default-threshold"))
                .isEqualTo("1000");
    }

    @Test
    void shouldConfigureSchedulingThreadPool() {
        assertThat(environment.getProperty("spring.task.scheduling.pool.size"))
                .isEqualTo("4");
        assertThat(environment.getProperty("spring.task.scheduling.thread-name-prefix"))
                .isEqualTo("hjo2oa-schedule-");
    }

    @Test
    void shouldConfigureActuatorManagementEndpoints() {
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health,info,readiness,liveness");
        assertThat(environment.getProperty("management.endpoints.web.base-path"))
                .isEqualTo("/actuator");
        assertThat(environment.getProperty("management.endpoint.health.show-details"))
                .isEqualTo("when-authorized");
        assertThat(environment.getProperty("management.health.readinessstate.enabled"))
                .isEqualTo("true");
        assertThat(environment.getProperty("management.health.livenessstate.enabled"))
                .isEqualTo("true");
    }
}
