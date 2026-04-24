package com.hjo2oa.data.common.infrastructure.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hjo2oa.data.common.application.audit.DataAuditLog;
import com.hjo2oa.data.common.domain.exception.DataServicesErrorCode;
import com.hjo2oa.data.common.domain.exception.DataServicesException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class DataAuditLogAspectTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldWriteSuccessAuditRecord() {
        CapturingDataAuditLogWriter writer = new CapturingDataAuditLogWriter();
        AuditedService target = createProxy(writer);
        bindRequestContext();

        String result = target.success("connector-1");

        assertEquals("ok:connector-1", result);
        assertEquals(1, writer.records.size());
        DataAuditRecord record = writer.records.get(0);
        assertEquals("connector", record.module());
        assertEquals("test-connection", record.action());
        assertTrue(record.success());
        assertEquals("tester", record.operatorId());
        assertEquals("audit-req-1", record.requestId());
        assertNotNull(record.detail());
    }

    @Test
    void shouldWriteFailureAuditRecordWithErrorCode() {
        CapturingDataAuditLogWriter writer = new CapturingDataAuditLogWriter();
        AuditedService target = createProxy(writer);
        bindRequestContext();

        try {
            target.fail();
        } catch (DataServicesException ex) {
            assertEquals(DataServicesErrorCode.CONNECTOR_NOT_ACTIVE, ex.dataErrorCode());
        }

        assertEquals(1, writer.records.size());
        DataAuditRecord record = writer.records.get(0);
        assertFalse(record.success());
        assertEquals(DataServicesErrorCode.CONNECTOR_NOT_ACTIVE.code(), record.errorCode());
    }

    private AuditedService createProxy(CapturingDataAuditLogWriter writer) {
        DataAuditLogAspect aspect = new DataAuditLogAspect(writer);
        AspectJProxyFactory factory = new AspectJProxyFactory(new AuditedService());
        factory.addAspect(aspect);
        return factory.getProxy();
    }

    private void bindRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "audit-req-1");
        request.addHeader("X-Tenant-Id", "22222222-2222-2222-2222-222222222222");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("tester", "N/A"));
    }

    static class CapturingDataAuditLogWriter implements DataAuditLogWriter {

        private final List<DataAuditRecord> records = new ArrayList<>();

        @Override
        public void write(DataAuditRecord record) {
            records.add(record);
        }
    }

    static class AuditedService {

        @DataAuditLog(
                module = "connector",
                action = "test-connection",
                targetType = "connector",
                captureArguments = true,
                captureResult = true
        )
        String success(String connectorCode) {
            return "ok:" + connectorCode;
        }

        @DataAuditLog(module = "connector", action = "disable", targetType = "connector", captureArguments = true)
        void fail() {
            throw new DataServicesException(DataServicesErrorCode.CONNECTOR_NOT_ACTIVE, "connector is disabled");
        }
    }
}
