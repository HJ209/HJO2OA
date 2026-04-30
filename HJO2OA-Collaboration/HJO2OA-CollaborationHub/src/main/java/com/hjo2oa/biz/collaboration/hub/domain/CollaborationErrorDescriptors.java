package com.hjo2oa.biz.collaboration.hub.domain;

import com.hjo2oa.shared.kernel.ErrorDescriptor;
import org.springframework.http.HttpStatus;

public final class CollaborationErrorDescriptors {

    public static final ErrorDescriptor FORBIDDEN =
            new ErrorDescriptor("BIZ_COLLAB_FORBIDDEN", HttpStatus.FORBIDDEN, "No collaboration permission");
    public static final ErrorDescriptor NOT_FOUND =
            new ErrorDescriptor("BIZ_COLLAB_NOT_FOUND", HttpStatus.NOT_FOUND, "Collaboration resource not found");
    public static final ErrorDescriptor CONFLICT =
            new ErrorDescriptor("BIZ_COLLAB_CONFLICT", HttpStatus.CONFLICT, "Collaboration resource conflict");
    public static final ErrorDescriptor BAD_REQUEST =
            new ErrorDescriptor("BIZ_COLLAB_BAD_REQUEST", HttpStatus.BAD_REQUEST, "Invalid collaboration request");
    public static final ErrorDescriptor INVALID_STATE =
            new ErrorDescriptor("BIZ_COLLAB_INVALID_STATE", HttpStatus.UNPROCESSABLE_ENTITY, "Invalid collaboration state");
    public static final ErrorDescriptor IDEMPOTENCY_REQUIRED =
            new ErrorDescriptor("BIZ_COLLAB_IDEMPOTENCY_REQUIRED", HttpStatus.BAD_REQUEST, "Idempotency key is required");

    private CollaborationErrorDescriptors() {
    }
}
