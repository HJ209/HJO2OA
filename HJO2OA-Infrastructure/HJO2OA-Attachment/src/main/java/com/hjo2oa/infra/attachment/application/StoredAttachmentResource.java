package com.hjo2oa.infra.attachment.application;

import org.springframework.core.io.Resource;

public record StoredAttachmentResource(
        Resource resource,
        long sizeBytes
) {
}
