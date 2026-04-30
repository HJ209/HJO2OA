package com.hjo2oa.infra.attachment.interfaces;

import com.hjo2oa.infra.attachment.domain.AttachmentAssetView;
import com.hjo2oa.infra.attachment.domain.AttachmentBindingView;
import com.hjo2oa.infra.attachment.domain.AttachmentVersionView;
import com.hjo2oa.infra.attachment.application.AttachmentAccessAuditRecord;
import com.hjo2oa.infra.attachment.application.AttachmentFileApplicationService;
import org.springframework.stereotype.Component;

@Component
public class AttachmentAssetDtoMapper {

    public AttachmentAssetDtos.AttachmentAssetResponse toResponse(AttachmentAssetView view) {
        return new AttachmentAssetDtos.AttachmentAssetResponse(
                view.id(),
                view.storageKey(),
                view.originalFilename(),
                view.contentType(),
                view.sizeBytes(),
                view.checksum(),
                view.storageProvider(),
                view.previewStatus(),
                view.latestVersionNo(),
                view.permissionMode(),
                view.tenantId(),
                view.createdBy(),
                view.createdAt(),
                view.updatedAt(),
                view.versions().stream().map(this::toResponse).toList(),
                view.bindings().stream().map(this::toResponse).toList()
        );
    }

    public AttachmentAssetDtos.AttachmentVersionResponse toResponse(AttachmentVersionView view) {
        return new AttachmentAssetDtos.AttachmentVersionResponse(
                view.id(),
                view.attachmentAssetId(),
                view.versionNo(),
                view.storageKey(),
                view.checksum(),
                view.sizeBytes(),
                view.createdBy(),
                view.createdAt()
        );
    }

    public AttachmentAssetDtos.AttachmentBindingResponse toResponse(AttachmentBindingView view) {
        return new AttachmentAssetDtos.AttachmentBindingResponse(
                view.id(),
                view.attachmentAssetId(),
                view.businessType(),
                view.businessId(),
                view.bindingRole(),
                view.active()
        );
    }

    public AttachmentAssetDtos.AttachmentPreviewResponse toResponse(
            AttachmentFileApplicationService.PreviewInfo previewInfo
    ) {
        return new AttachmentAssetDtos.AttachmentPreviewResponse(
                previewInfo.assetId(),
                previewInfo.previewStatus(),
                previewInfo.previewAvailable(),
                previewInfo.contentType(),
                previewInfo.downloadUrl()
        );
    }

    public AttachmentAssetDtos.AttachmentAccessAuditResponse toResponse(AttachmentAccessAuditRecord record) {
        return new AttachmentAssetDtos.AttachmentAccessAuditResponse(
                record.id(),
                record.attachmentId(),
                record.versionNo(),
                record.action(),
                record.tenantId(),
                record.operatorId(),
                record.clientIp(),
                record.occurredAt()
        );
    }
}
