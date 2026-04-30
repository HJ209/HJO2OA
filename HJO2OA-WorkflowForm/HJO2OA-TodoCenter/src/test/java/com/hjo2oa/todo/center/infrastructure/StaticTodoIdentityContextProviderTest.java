package com.hjo2oa.todo.center.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.shared.tenant.SharedTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class StaticTodoIdentityContextProviderTest {

    private final StaticTodoIdentityContextProvider provider = new StaticTodoIdentityContextProvider();

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldResolveTodoIdentityFromRequestHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SharedTenant.TENANT_ID_HEADER, "tenant-20260429");
        request.addHeader("X-Person-Id", "person-20260429");
        request.addHeader(SharedTenant.IDENTITY_ASSIGNMENT_ID_HEADER, "assignment-20260429");
        request.addHeader(SharedTenant.IDENTITY_POSITION_ID_HEADER, "position-20260429");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        var context = provider.currentContext();

        assertThat(context.tenantId()).isEqualTo("tenant-20260429");
        assertThat(context.personId()).isEqualTo("person-20260429");
        assertThat(context.assignmentId()).isEqualTo("assignment-20260429");
        assertThat(context.positionId()).isEqualTo("position-20260429");
    }

    @Test
    void shouldFallbackWhenNoRequestIsBound() {
        var context = provider.currentContext();

        assertThat(context.tenantId()).isEqualTo("tenant-1");
        assertThat(context.personId()).isEqualTo("person-1");
        assertThat(context.assignmentId()).isEqualTo("assignment-1");
        assertThat(context.positionId()).isEqualTo("position-1");
    }
}
