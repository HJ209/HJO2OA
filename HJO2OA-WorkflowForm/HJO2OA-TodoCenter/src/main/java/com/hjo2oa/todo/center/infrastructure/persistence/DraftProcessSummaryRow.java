package com.hjo2oa.todo.center.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DraftProcessSummaryRow {

    private UUID submissionId;
    private UUID metadataId;
    private String metadataCode;
    private Integer metadataVersion;
    private UUID processInstanceId;
    private String nodeId;
    private Instant createdAt;
    private Instant updatedAt;
}
