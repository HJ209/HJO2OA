package com.hjo2oa.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.process.monitor.domain.ProcessMonitorQueryRepository;
import com.hjo2oa.process.monitor.infrastructure.ProcessMonitorQueryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = {Hjo2oaApplication.class, BootstrapContextTestConfiguration.class},
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
class ProcessMonitorBeanAssemblyTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private ProcessMonitorQueryMapper processMonitorQueryMapper;

    @Test
    @DisplayName("ProcessMonitor query repository should be assembled in Bootstrap context")
    void shouldLoadProcessMonitorQueryRepository() {
        assertThat(applicationContext.getBean(ProcessMonitorQueryRepository.class)).isNotNull();
    }
}
