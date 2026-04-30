package com.hjo2oa.shared.tenant;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record TenantRequestContext(
        UUID tenantId,
        String requestId,
        String idempotencyKey,
        Locale language,
        ZoneId timezone,
        UUID identityAssignmentId,
        UUID identityPositionId
) {

    private static final Locale DEFAULT_LANGUAGE = Locale.forLanguageTag("zh-CN");
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("UTC");

    public TenantRequestContext {
        requestId = normalizeOptional(requestId);
        idempotencyKey = normalizeOptional(idempotencyKey);
        language = language == null ? DEFAULT_LANGUAGE : language;
        timezone = timezone == null ? DEFAULT_TIMEZONE : timezone;
    }

    public static Builder builder() {
        return new Builder();
    }

    public TenantRequestContext requireTenant() {
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context is missing X-Tenant-Id");
        }
        return this;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static final class Builder {

        private UUID tenantId;
        private String requestId;
        private String idempotencyKey;
        private Locale language;
        private ZoneId timezone;
        private UUID identityAssignmentId;
        private UUID identityPositionId;

        private Builder() {
        }

        public Builder tenantId(UUID tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder tenantId(String tenantId) {
            this.tenantId = parseUuid(tenantId, SharedTenant.TENANT_ID_HEADER);
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder language(String language) {
            String normalized = normalizeOptional(language);
            String languageTag = normalized == null ? null : normalized.split(",", 2)[0].trim();
            this.language = languageTag == null || languageTag.isBlank()
                    ? DEFAULT_LANGUAGE
                    : Locale.forLanguageTag(languageTag);
            return this;
        }

        public Builder timezone(String timezone) {
            String normalized = normalizeOptional(timezone);
            if (normalized == null) {
                this.timezone = DEFAULT_TIMEZONE;
                return this;
            }
            try {
                this.timezone = ZoneId.of(normalized);
            } catch (DateTimeException ex) {
                throw new IllegalArgumentException("Invalid X-Timezone header: " + normalized, ex);
            }
            return this;
        }

        public Builder identityAssignmentId(String identityAssignmentId) {
            this.identityAssignmentId = parseUuid(
                    identityAssignmentId,
                    SharedTenant.IDENTITY_ASSIGNMENT_ID_HEADER
            );
            return this;
        }

        public Builder identityPositionId(String identityPositionId) {
            this.identityPositionId = parseUuid(identityPositionId, SharedTenant.IDENTITY_POSITION_ID_HEADER);
            return this;
        }

        public TenantRequestContext build() {
            return new TenantRequestContext(
                    tenantId,
                    requestId,
                    idempotencyKey,
                    language,
                    timezone,
                    identityAssignmentId,
                    identityPositionId
            );
        }

        private static UUID parseUuid(String value, String fieldName) {
            String normalized = normalizeOptional(value);
            if (normalized == null) {
                return null;
            }
            try {
                return UUID.fromString(normalized);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(fieldName + " must be a UUID", ex);
            }
        }
    }

    public static TenantRequestContext merge(TenantRequestContext current, TenantRequestContext fallback) {
        if (current == null) {
            return fallback;
        }
        if (fallback == null) {
            return current;
        }
        return new TenantRequestContext(
                current.tenantId == null ? fallback.tenantId : current.tenantId,
                Objects.requireNonNullElse(current.requestId, fallback.requestId),
                Objects.requireNonNullElse(current.idempotencyKey, fallback.idempotencyKey),
                current.language == null ? fallback.language : current.language,
                current.timezone == null ? fallback.timezone : current.timezone,
                current.identityAssignmentId == null ? fallback.identityAssignmentId : current.identityAssignmentId,
                current.identityPositionId == null ? fallback.identityPositionId : current.identityPositionId
        );
    }
}
