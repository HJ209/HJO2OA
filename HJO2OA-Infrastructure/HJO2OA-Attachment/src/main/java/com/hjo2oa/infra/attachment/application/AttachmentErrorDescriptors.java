package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.http.HttpStatus;

public final class AttachmentErrorDescriptors {

    public static final ErrorDescriptor ATTACHMENT_NOT_FOUND =
            SharedErrorDescriptors.of("INFRA_ATTACHMENT_NOT_FOUND", HttpStatus.NOT_FOUND, "Attachment not found");
    public static final ErrorDescriptor ATTACHMENT_ACCESS_DENIED =
            SharedErrorDescriptors.of("INFRA_ATTACHMENT_ACCESS_DENIED", HttpStatus.FORBIDDEN, "Attachment access denied");
    public static final ErrorDescriptor ATTACHMENT_SIZE_EXCEEDED =
            SharedErrorDescriptors.of(
                    "INFRA_ATTACHMENT_SIZE_EXCEEDED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Attachment size exceeds limit"
            );
    public static final ErrorDescriptor ATTACHMENT_TYPE_UNSUPPORTED =
            SharedErrorDescriptors.of(
                    "INFRA_ATTACHMENT_TYPE_UNSUPPORTED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Attachment content type is unsupported"
            );
    public static final ErrorDescriptor ATTACHMENT_STORAGE_FAILED =
            SharedErrorDescriptors.of(
                    "INFRA_ATTACHMENT_STORAGE_FAILED",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Attachment storage failed"
            );
    public static final ErrorDescriptor ATTACHMENT_VIRUS_SCAN_REJECTED =
            SharedErrorDescriptors.of(
                    "INFRA_ATTACHMENT_VIRUS_SCAN_REJECTED",
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Attachment virus scan rejected the file"
            );

    private AttachmentErrorDescriptors() {
    }
}
