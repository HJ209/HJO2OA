package com.hjo2oa.wf.process.definition.domain;

import java.time.Instant;
import java.util.UUID;

public record ProcessDefinitionView(
        UUID id,
        String code,
        String name,
        String category,
        int version,
        DefinitionStatus status,
        UUID formMetadataId,
        String startNodeId,
        String endNodeId,
        String nodes,
        String routes,
        UUID tenantId,
        Instant publishedAt,
        UUID publishedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
