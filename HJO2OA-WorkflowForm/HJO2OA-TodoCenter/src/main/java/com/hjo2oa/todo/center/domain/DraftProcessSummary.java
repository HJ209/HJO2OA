package com.hjo2oa.todo.center.domain;

import java.time.Instant;
import java.util.UUID;

public record DraftProcessSummary(
        UUID submissionId,
        UUID metadataId,
        String metadataCode,
        int metadataVersion,
        UUID processInstanceId,
        String nodeId,
        Instant createdAt,
        Instant updatedAt
) {
}
