package com.hjo2oa.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.org.org.sync.audit.domain.AuditRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.CompensationRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.DiffRecordRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncSourceConfigRepository;
import com.hjo2oa.org.org.sync.audit.domain.SyncTaskRepository;
import com.hjo2oa.org.org.sync.audit.infrastructure.OrgSyncAuditRecordMapper;
import com.hjo2oa.org.org.sync.audit.infrastructure.CompensationRecordMapper;
import com.hjo2oa.org.org.sync.audit.infrastructure.DiffRecordMapper;
import com.hjo2oa.org.org.sync.audit.infrastructure.SyncSourceConfigMapper;
import com.hjo2oa.org.org.sync.audit.infrastructure.SyncTaskMapper;
import javax.sql.DataSource;
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
class OrgSyncAuditBeanAssemblyTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private DataSource dataSource;

    @MockBean
    private SyncSourceConfigMapper syncSourceConfigMapper;

    @MockBean
    private SyncTaskMapper syncTaskMapper;

    @MockBean
    private OrgSyncAuditRecordMapper auditRecordMapper;

    @MockBean
    private DiffRecordMapper diffRecordMapper;

    @MockBean
    private CompensationRecordMapper compensationRecordMapper;

    @Test
    @DisplayName("OrgSyncAudit repositories should be assembled in Bootstrap context")
    void shouldLoadOrgSyncAuditRepositories() {
        assertThat(applicationContext.getBean(SyncSourceConfigRepository.class)).isNotNull();
        assertThat(applicationContext.getBean(SyncTaskRepository.class)).isNotNull();
        assertThat(applicationContext.getBean(AuditRecordRepository.class)).isNotNull();
        assertThat(applicationContext.getBean(DiffRecordRepository.class)).isNotNull();
        assertThat(applicationContext.getBean(CompensationRecordRepository.class)).isNotNull();
    }
}
