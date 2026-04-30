package com.hjo2oa.infra.audit.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.audit.domain.AuditQuery;
import com.hjo2oa.infra.audit.domain.AuditRecordView;
import com.hjo2oa.infra.audit.infrastructure.InMemoryAuditRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuditAspectTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T09:00:00Z");

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldRecordAnnotatedWriteOperationWithRequestContextAndBeforeAfterSummary() {
        AuditRecordApplicationService applicationService = new AuditRecordApplicationService(
                new InMemoryAuditRecordRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        AuditAspect aspect = new AuditAspect(applicationService);
        AuditedTarget target = new AuditedTarget();
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAspect(aspect);
        AuditedTarget proxy = proxyFactory.getProxy();
        UUID jobId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID tenantId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        UUID operatorAccountId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/infra/scheduler/jobs/" + jobId);
        request.addHeader("X-Tenant-Id", tenantId.toString());
        request.addHeader("X-Operator-Account-Id", operatorAccountId.toString());
        request.addHeader("X-Request-Id", "req-aop-1");
        request.addHeader("Accept-Language", "zh-CN");
        request.addHeader("X-Timezone", "Asia/Shanghai");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        UpdateResult result = proxy.update(jobId, tenantId, "active");

        assertThat(result.id()).isEqualTo(jobId);
        List<AuditRecordView> records = applicationService.queryAudits(new AuditQuery(
                tenantId,
                "scheduler",
                "ScheduledJob",
                jobId.toString(),
                "UPDATE",
                operatorAccountId,
                null,
                "req-aop-1",
                FIXED_TIME.minusSeconds(1),
                FIXED_TIME.plusSeconds(1)
        ));

        assertThat(records).singleElement().satisfies(record -> {
            assertThat(record.traceId()).isEqualTo("req-aop-1");
            assertThat(record.operatorAccountId()).isEqualTo(operatorAccountId);
            assertThat(record.fieldChanges()).extracting("fieldName")
                    .contains("before", "after", "requestId", "language", "timezone");
        });
    }

    static class AuditedTarget {

        @Audited(module = "scheduler", action = "UPDATE", targetType = "ScheduledJob", targetId = "#jobId")
        public UpdateResult update(UUID jobId, UUID tenantId, String status) {
            return new UpdateResult(jobId, tenantId, status);
        }
    }

    record UpdateResult(UUID id, UUID tenantId, String status) {
    }
}
