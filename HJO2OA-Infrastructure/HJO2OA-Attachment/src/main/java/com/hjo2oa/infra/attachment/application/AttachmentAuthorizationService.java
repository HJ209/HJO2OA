package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.AttachmentAsset;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AttachmentAuthorizationService {

    public void assertTenantAccess(AttachmentAsset asset) {
        UUID tenantId = TenantContextHolder.currentTenantId().orElse(null);
        if (tenantId != null && !tenantId.equals(asset.tenantId())) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_ACCESS_DENIED,
                    "Attachment belongs to another tenant"
            );
        }
    }

    public void assertBusinessAccess(AttachmentAsset asset, String businessType, String businessId) {
        assertTenantAccess(asset);
        if (businessType == null || businessType.isBlank() || businessId == null || businessId.isBlank()) {
            return;
        }
        boolean bound = asset.bindings().stream()
                .anyMatch(binding -> binding.active()
                        && binding.businessType().equals(businessType)
                        && binding.businessId().equals(businessId));
        if (!bound) {
            throw new BizException(
                    AttachmentErrorDescriptors.ATTACHMENT_ACCESS_DENIED,
                    "Attachment is not bound to requested business object"
            );
        }
    }

    public UUID resolveRequiredTenant(UUID requestTenantId) {
        UUID currentTenantId = TenantContextHolder.currentTenantId().orElse(null);
        UUID tenantId = requestTenantId == null ? currentTenantId : requestTenantId;
        return Objects.requireNonNull(tenantId, "tenantId must not be null");
    }
}
