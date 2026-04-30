package com.hjo2oa.infra.attachment.application;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AttachmentUploadPolicy {

    private static final List<String> DEFAULT_ALLOWED_TYPES = List.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/gif",
            "text/plain",
            "text/csv",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final long maxSizeBytes;
    private final List<String> allowedContentTypes;

    public AttachmentUploadPolicy(
            @Value("${hjo2oa.infra.attachment.max-size-bytes:52428800}") long maxSizeBytes,
            @Value("${hjo2oa.infra.attachment.allowed-content-types:}") List<String> allowedContentTypes
    ) {
        if (maxSizeBytes <= 0) {
            throw new IllegalArgumentException("maxSizeBytes must be positive");
        }
        this.maxSizeBytes = maxSizeBytes;
        List<String> configuredTypes = allowedContentTypes == null ? List.of() : allowedContentTypes;
        this.allowedContentTypes = configuredTypes.isEmpty()
                ? DEFAULT_ALLOWED_TYPES
                : configuredTypes.stream().filter(Objects::nonNull).map(this::normalize).toList();
    }

    public void validate(String contentType, long sizeBytes) {
        if (sizeBytes > maxSizeBytes) {
            throw new com.hjo2oa.shared.kernel.BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_SIZE_EXCEEDED,
                    "Attachment size " + sizeBytes + " exceeds limit " + maxSizeBytes
            );
        }
        String normalizedContentType = normalize(contentType);
        boolean allowed = allowedContentTypes.stream().anyMatch(allowedType -> allowedType.equals(normalizedContentType));
        if (!allowed) {
            throw new com.hjo2oa.shared.kernel.BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_TYPE_UNSUPPORTED,
                    "Attachment content type unsupported: " + contentType
            );
        }
    }

    private String normalize(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }
}
