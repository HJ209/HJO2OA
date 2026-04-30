package com.hjo2oa.shared.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SharedTenantLineHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @AfterEach
    void cleanUp() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldIgnoreGlobalTablesAndEnforceTenantScopedTables() {
        SharedTenantLineHandler handler = new SharedTenantLineHandler();

        assertThat(handler.ignoreTable("infra_tenant_profile")).isTrue();
        assertThat(handler.ignoreTable("infra_config_entry")).isTrue();
        assertThat(handler.ignoreTable("org_organization")).isFalse();
        assertThat(handler.ignoreTable("dbo.org_person")).isFalse();
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }

    @Test
    void shouldReadTenantIdFromThreadContextForTenantLineExpression() {
        SharedTenantLineHandler handler = new SharedTenantLineHandler();

        try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(TenantRequestContext.builder()
                .tenantId(TENANT_ID)
                .build())) {
            assertThat(handler.getTenantId().toString()).contains(TENANT_ID.toString());
        }
    }

    @Test
    void shouldBuildDifferentTenantPredicatesForDifferentTenants() {
        SharedTenantLineHandler handler = new SharedTenantLineHandler();
        UUID anotherTenantId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        String firstExpression;
        try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(TenantRequestContext.builder()
                .tenantId(TENANT_ID)
                .build())) {
            firstExpression = handler.getTenantId().toString();
        }
        String secondExpression;
        try (TenantContextHolder.Scope ignored = TenantContextHolder.bind(TenantRequestContext.builder()
                .tenantId(anotherTenantId)
                .build())) {
            secondExpression = handler.getTenantId().toString();
        }

        assertThat(firstExpression).contains(TENANT_ID.toString());
        assertThat(secondExpression).contains(anotherTenantId.toString());
        assertThat(firstExpression).isNotEqualTo(secondExpression);
    }

    @Test
    void shouldFailFastWhenTenantScopedQueryHasNoTenantContext() {
        SharedTenantLineHandler handler = new SharedTenantLineHandler();

        assertThatThrownBy(handler::getTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Tenant context");
    }
}
