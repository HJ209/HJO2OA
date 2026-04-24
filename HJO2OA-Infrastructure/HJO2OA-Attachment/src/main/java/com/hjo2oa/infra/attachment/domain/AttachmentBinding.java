package com.hjo2oa.infra.attachment.domain;

import java.util.Objects;
import java.util.UUID;

public record AttachmentBinding(
        UUID id,
        UUID attachmentAssetId,
        String businessType,
        String businessId,
        BindingRole bindingRole,
        boolean active
) {

    public AttachmentBinding {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(attachmentAssetId, "attachmentAssetId must not be null");
        businessType = requireText(businessType, "businessType");
        businessId = requireText(businessId, "businessId");
        Objects.requireNonNull(bindingRole, "bindingRole must not be null");
    }

    public static AttachmentBinding create(
            UUID attachmentAssetId,
            String businessType,
            String businessId,
            BindingRole bindingRole
    ) {
        return new AttachmentBinding(
                UUID.randomUUID(),
                attachmentAssetId,
                businessType,
                businessId,
                bindingRole,
                true
        );
    }

    public boolean matches(
            String businessType,
            String businessId,
            BindingRole bindingRole
    ) {
        return this.businessType.equals(businessType)
                && this.businessId.equals(businessId)
                && this.bindingRole == bindingRole;
    }

    public AttachmentBinding activate() {
        if (active) {
            return this;
        }
        return new AttachmentBinding(id, attachmentAssetId, businessType, businessId, bindingRole, true);
    }

    public AttachmentBinding deactivate() {
        if (!active) {
            return this;
        }
        return new AttachmentBinding(id, attachmentAssetId, businessType, businessId, bindingRole, false);
    }

    public AttachmentBindingView toView() {
        return new AttachmentBindingView(id, attachmentAssetId, businessType, businessId, bindingRole, active);
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
