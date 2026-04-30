package com.hjo2oa.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TenantContextHolderTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldBindTenantHeadersForRequestAndClearAfterFilter() throws Exception {
        TenantContextFilter filter = new TenantContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SharedTenant.TENANT_ID_HEADER, TENANT_ID.toString());
        request.addHeader(SharedTenant.REQUEST_ID_HEADER, "req-tenant-context");
        request.addHeader(SharedTenant.IDEMPOTENCY_KEY_HEADER, "idem-tenant-context");
        request.addHeader(SharedTenant.LANGUAGE_HEADER, "en-US,en;q=0.9");
        request.addHeader(SharedTenant.TIMEZONE_HEADER, "Asia/Shanghai");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
                    throws ServletException, IOException {
                TenantRequestContext context = TenantContextHolder.requireContext();

                assertThat(context.tenantId()).isEqualTo(TENANT_ID);
                assertThat(context.requestId()).isEqualTo("req-tenant-context");
                assertThat(context.idempotencyKey()).isEqualTo("idem-tenant-context");
                assertThat(context.language()).isEqualTo(Locale.forLanguageTag("en-US"));
                assertThat(context.timezone()).isEqualTo(ZoneId.of("Asia/Shanghai"));
            }
        }));

        assertThat(TenantContextHolder.current()).isEmpty();
    }

    @Test
    void shouldReturnApiResponseForInvalidTenantHeader() throws Exception {
        TenantContextFilter filter = new TenantContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SharedTenant.TENANT_ID_HEADER, "not-a-uuid");
        request.addHeader(SharedTenant.REQUEST_ID_HEADER, "req-invalid-tenant");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
                throw new AssertionError("Filter chain must not be invoked for invalid tenant header");
            }
        }));

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_BAD_REQUEST);
        assertThat(response.getContentAsString())
                .contains("\"code\":\"BAD_REQUEST\"")
                .contains("\"requestId\":\"req-invalid-tenant\"");
        assertThat(TenantContextHolder.current()).isEmpty();
    }

    @Test
    void shouldPropagateTenantContextToWrappedAsyncTaskAndRestorePreviousContext() {
        UUID previousTenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        TenantRequestContext capturedContext = TenantRequestContext.builder()
                .tenantId(TENANT_ID)
                .requestId("req-captured")
                .build();
        TenantRequestContext previousContext = TenantRequestContext.builder()
                .tenantId(previousTenantId)
                .requestId("req-previous")
                .build();

        Runnable wrapped;
        try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(capturedContext)) {
            wrapped = TenantContextHolder.wrap((Runnable) () -> assertThat(TenantContextHolder.requireTenantId())
                    .isEqualTo(TENANT_ID));
        }

        try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(previousContext)) {
            wrapped.run();
            assertThat(TenantContextHolder.requireTenantId()).isEqualTo(previousTenantId);
        }
    }
}
