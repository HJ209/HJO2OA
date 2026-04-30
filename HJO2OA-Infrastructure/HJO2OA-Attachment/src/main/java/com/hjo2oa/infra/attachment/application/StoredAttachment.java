package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.StorageProvider;
import java.nio.file.Path;

public record StoredAttachment(
        String storageKey,
        long sizeBytes,
        String checksum,
        StorageProvider storageProvider,
        Path localPath
) {
}
