package com.hjo2oa.data.openapi.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

public record ApiRateLimitPolicy(
        String policyId,
        String openApiId,
        String tenantId,
        String policyCode,
        String clientCode,
        ApiPolicyType policyType,
        long windowValue,
        ApiWindowUnit windowUnit,
        long threshold,
        ApiPolicyStatus status,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public ApiRateLimitPolicy {
        policyId = requireText(policyId, "policyId");
        openApiId = requireText(openApiId, "openApiId");
        tenantId = requireText(tenantId, "tenantId");
        policyCode = requireText(policyCode, "policyCode");
        clientCode = normalizeNullable(clientCode);
        Objects.requireNonNull(policyType, "policyType must not be null");
        if (windowValue <= 0) {
            throw new IllegalArgumentException("windowValue must be greater than 0");
        }
        Objects.requireNonNull(windowUnit, "windowUnit must not be null");
        if (threshold <= 0) {
            throw new IllegalArgumentException("threshold must be greater than 0");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static ApiRateLimitPolicy create(
            String openApiId,
            String tenantId,
            String policyCode,
            String clientCode,
            ApiPolicyType policyType,
            long windowValue,
            ApiWindowUnit windowUnit,
            long threshold,
            String description,
            Instant now
    ) {
        return new ApiRateLimitPolicy(
                UUID.randomUUID().toString(),
                openApiId,
                tenantId,
                policyCode,
                clientCode,
                policyType,
                windowValue,
                windowUnit,
                threshold,
                ApiPolicyStatus.ACTIVE,
                description,
                now,
                now
        );
    }

    public ApiRateLimitPolicy update(
            String clientCode,
            ApiPolicyType policyType,
            long windowValue,
            ApiWindowUnit windowUnit,
            long threshold,
            String description,
            Instant now
    ) {
        return new ApiRateLimitPolicy(
                policyId,
                openApiId,
                tenantId,
                policyCode,
                clientCode,
                policyType,
                windowValue,
                windowUnit,
                threshold,
                ApiPolicyStatus.ACTIVE,
                description,
                createdAt,
                now
        );
    }

    public ApiRateLimitPolicy disable(Instant now) {
        return new ApiRateLimitPolicy(
                policyId,
                openApiId,
                tenantId,
                policyCode,
                clientCode,
                policyType,
                windowValue,
                windowUnit,
                threshold,
                ApiPolicyStatus.DISABLED,
                description,
                createdAt,
                now
        );
    }

    public boolean appliesToClient(String invokingClientCode) {
        return clientCode == null || clientCode.equals(invokingClientCode);
    }

    public Instant windowStartedAt(Instant now) {
        if (windowUnit == ApiWindowUnit.MONTH) {
            ZonedDateTime zonedDateTime = now.atZone(ZoneOffset.UTC);
            int monthsFromEpoch = zonedDateTime.getYear() * 12 + zonedDateTime.getMonthValue() - 1;
            int bucketStart = (int) ((monthsFromEpoch / windowValue) * windowValue);
            int year = Math.floorDiv(bucketStart, 12);
            int month = Math.floorMod(bucketStart, 12) + 1;
            return YearMonth.of(year, month).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        long unitSeconds = switch (windowUnit) {
            case SECOND -> 1L;
            case MINUTE -> 60L;
            case HOUR -> 3600L;
            case DAY -> 86400L;
            case MONTH -> throw new IllegalStateException("month handled separately");
        };
        long bucketSize = unitSeconds * windowValue;
        long epochSecond = now.getEpochSecond();
        long windowStart = (epochSecond / bucketSize) * bucketSize;
        return Instant.ofEpochSecond(windowStart);
    }

    public boolean isEnabled() {
        return status == ApiPolicyStatus.ACTIVE;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
