package com.hjo2oa.shared.tenant;

public final class SharedTenant {

    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    public static final String LANGUAGE_HEADER = "Accept-Language";
    public static final String TIMEZONE_HEADER = "X-Timezone";
    public static final String IDENTITY_ASSIGNMENT_ID_HEADER = "X-Identity-Assignment-Id";
    public static final String IDENTITY_POSITION_ID_HEADER = "X-Identity-Position-Id";

    private SharedTenant() {
    }
}
