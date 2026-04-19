package com.hjo2oa.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
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
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,"
                        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration"
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
                .isEqualTo("jdbc:postgresql://localhost:5432/hjo2oa_local");
        assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("hjo2oa.storage.endpoint"))
                .isEqualTo("http://localhost:9000");
        assertThat(environment.getProperty("hjo2oa.storage.bucket")).isEqualTo("hjo2oa-local");
    }
}
