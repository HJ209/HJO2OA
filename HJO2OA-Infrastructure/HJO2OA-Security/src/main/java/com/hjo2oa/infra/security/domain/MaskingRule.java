package com.hjo2oa.infra.security.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record MaskingRule(
        UUID id,
        UUID securityPolicyId,
        String dataType,
        String ruleExpr,
        boolean active
) {

    public MaskingRule {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(securityPolicyId, "securityPolicyId must not be null");
        dataType = requireText(dataType, "dataType");
        ruleExpr = requireText(ruleExpr, "ruleExpr");
    }

    public static MaskingRule create(UUID id, UUID securityPolicyId, String dataType, String ruleExpr) {
        return new MaskingRule(id, securityPolicyId, dataType, ruleExpr, true);
    }

    public boolean matches(String candidateDataType) {
        return active && dataType.equalsIgnoreCase(requireText(candidateDataType, "candidateDataType"));
    }

    public String mask(String value) {
        if (value == null) {
            return null;
        }
        String expression = ruleExpr.trim().toUpperCase(Locale.ROOT);
        if (expression.startsWith("KEEP_PREFIX(") && expression.endsWith(")")) {
            int keep = parsePositiveInt(expression.substring("KEEP_PREFIX(".length(), expression.length() - 1));
            return keepAround(value, keep, 0);
        }
        if (expression.startsWith("KEEP_SUFFIX(") && expression.endsWith(")")) {
            int keep = parsePositiveInt(expression.substring("KEEP_SUFFIX(".length(), expression.length() - 1));
            return keepAround(value, 0, keep);
        }
        if (expression.startsWith("KEEP_BOTH(") && expression.endsWith(")")) {
            String[] parts = expression.substring("KEEP_BOTH(".length(), expression.length() - 1).split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("ruleExpr KEEP_BOTH must contain prefix and suffix length");
            }
            return keepAround(value, parsePositiveInt(parts[0]), parsePositiveInt(parts[1]));
        }
        if ("EMAIL".equals(expression)) {
            return maskEmail(value);
        }
        if ("MASK_ALL".equals(expression) || "FULL".equals(expression)) {
            return maskAll(value);
        }
        return maskAll(value);
    }

    public MaskingRuleView toView() {
        return new MaskingRuleView(id, securityPolicyId, dataType, ruleExpr, active);
    }

    private String keepAround(String value, int prefix, int suffix) {
        if (value.isEmpty() || prefix + suffix >= value.length()) {
            return value;
        }
        int maskLength = value.length() - prefix - suffix;
        return value.substring(0, prefix) + "*".repeat(maskLength) + value.substring(value.length() - suffix);
    }

    private String maskEmail(String value) {
        int delimiter = value.indexOf('@');
        if (delimiter <= 0) {
            return maskAll(value);
        }
        String localPart = value.substring(0, delimiter);
        String domainPart = value.substring(delimiter);
        if (localPart.length() == 1) {
            return "*" + domainPart;
        }
        return localPart.charAt(0) + "*".repeat(localPart.length() - 1) + domainPart;
    }

    private String maskAll(String value) {
        return "*".repeat(Math.max(4, value.length()));
    }

    private static int parsePositiveInt(String value) {
        int parsed = Integer.parseInt(value.trim());
        if (parsed < 0) {
            throw new IllegalArgumentException("ruleExpr value must not be negative");
        }
        return parsed;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
