package com.hjo2oa.msg.message.center.domain;

import java.util.Objects;

public record MessageIdentityContext(
        String tenantId,
        String recipientId,
        String assignmentId,
        String positionId
) {

    public MessageIdentityContext {
        tenantId = requireText(tenantId, "tenantId");
        recipientId = requireText(recipientId, "recipientId");
        assignmentId = requireText(assignmentId, "assignmentId");
        positionId = requireText(positionId, "positionId");
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
