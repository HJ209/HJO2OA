package com.hjo2oa.wf.action.engine.domain;

import java.util.Objects;

public record ActionOperator(
        String accountId,
        String personId,
        String positionId,
        String orgId
) {

    public ActionOperator {
        accountId = requireText(accountId, "accountId");
        personId = normalize(positionId == null ? personId : personId);
        positionId = normalize(positionId);
        orgId = normalize(orgId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
