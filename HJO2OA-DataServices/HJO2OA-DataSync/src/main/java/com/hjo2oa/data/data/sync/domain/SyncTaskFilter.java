package com.hjo2oa.data.data.sync.domain;

import java.util.UUID;

public record SyncTaskFilter(
        UUID tenantId,
        String code,
        SyncMode syncMode,
        SyncTaskStatus status,
        UUID sourceConnectorId,
        UUID targetConnectorId,
        int page,
        int size
) {

    public SyncTaskFilter {
        code = SyncDomainSupport.normalize(code);
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than 0");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be greater than 0");
        }
    }
}
