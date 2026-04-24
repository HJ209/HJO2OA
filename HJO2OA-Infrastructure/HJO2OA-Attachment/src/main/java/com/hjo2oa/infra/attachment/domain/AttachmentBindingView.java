package com.hjo2oa.infra.attachment.domain;

import java.util.UUID;

public record AttachmentBindingView(
        UUID id,
        UUID attachmentAssetId,
        String businessType,
        String businessId,
        BindingRole bindingRole,
        boolean active
) {
}
