package com.hjo2oa.infra.attachment.application;

import com.hjo2oa.infra.attachment.domain.AttachmentAsset;
import com.hjo2oa.infra.attachment.domain.AttachmentAssetRepository;
import com.hjo2oa.infra.attachment.domain.AttachmentAssetView;
import com.hjo2oa.infra.attachment.domain.AttachmentCreatedEvent;
import com.hjo2oa.infra.attachment.domain.AttachmentQuotaWarningEvent;
import com.hjo2oa.infra.attachment.domain.AttachmentVersionCreatedEvent;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AttachmentAssetApplicationService {

    private final AttachmentAssetRepository attachmentAssetRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    private final long quotaWarningThresholdBytes;

    @Autowired
    public AttachmentAssetApplicationService(
            AttachmentAssetRepository attachmentAssetRepository,
            DomainEventPublisher domainEventPublisher,
            @Value("${hjo2oa.infra.attachment.quota-warning-threshold-bytes:9223372036854775807}")
            long quotaWarningThresholdBytes
    ) {
        this(attachmentAssetRepository, domainEventPublisher, Clock.systemUTC(), quotaWarningThresholdBytes);
    }

    public AttachmentAssetApplicationService(
            AttachmentAssetRepository attachmentAssetRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(attachmentAssetRepository, domainEventPublisher, clock, Long.MAX_VALUE);
    }

    public AttachmentAssetApplicationService(
            AttachmentAssetRepository attachmentAssetRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock,
            long quotaWarningThresholdBytes
    ) {
        this.attachmentAssetRepository = Objects.requireNonNull(
                attachmentAssetRepository,
                "attachmentAssetRepository must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(
                domainEventPublisher,
                "domainEventPublisher must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.quotaWarningThresholdBytes = quotaWarningThresholdBytes;
    }

    public Optional<AttachmentAssetView> current(UUID assetId) {
        Objects.requireNonNull(assetId, "assetId must not be null");
        return attachmentAssetRepository.findById(assetId).map(AttachmentAsset::toView);
    }

    public AttachmentAssetView createAsset(AttachmentAssetCommands.CreateAssetCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        attachmentAssetRepository.findByStorageKey(command.storageKey())
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Attachment storage key already exists");
                });

        long previousTotal = attachmentAssetRepository.sumSizeBytesByTenant(command.tenantId());
        Instant now = now();
        AttachmentAsset attachmentAsset = AttachmentAsset.create(
                command.storageKey(),
                command.originalFilename(),
                command.contentType(),
                command.sizeBytes(),
                command.checksum(),
                command.storageProvider(),
                command.tenantId(),
                command.createdBy(),
                now
        );
        attachmentAssetRepository.save(attachmentAsset);
        domainEventPublisher.publish(AttachmentCreatedEvent.from(attachmentAsset, now));
        publishQuotaWarningIfNeeded(
                command.tenantId(),
                attachmentAsset.id(),
                previousTotal,
                previousTotal + attachmentAsset.sizeBytes(),
                now
        );
        return attachmentAsset.toView();
    }

    public AttachmentAssetView addVersion(AttachmentAssetCommands.AddVersionCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AttachmentAsset attachmentAsset = requireAsset(command.assetId());
        attachmentAssetRepository.findByStorageKey(command.storageKey())
                .filter(existing -> !existing.id().equals(command.assetId()))
                .ifPresent(existing -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Attachment storage key already exists");
                });

        long previousTotal = attachmentAssetRepository.sumSizeBytesByTenant(attachmentAsset.tenantId());
        Instant now = now();
        AttachmentAsset updatedAsset = attachmentAsset.addVersion(
                command.storageKey(),
                command.checksum(),
                command.sizeBytes(),
                command.createdBy(),
                now
        );
        attachmentAssetRepository.save(updatedAsset);
        domainEventPublisher.publish(AttachmentVersionCreatedEvent.from(updatedAsset, now));
        long currentTotal = previousTotal - attachmentAsset.sizeBytes() + updatedAsset.sizeBytes();
        publishQuotaWarningIfNeeded(
                updatedAsset.tenantId(),
                updatedAsset.id(),
                previousTotal,
                currentTotal,
                now
        );
        return updatedAsset.toView();
    }

    public AttachmentAssetView bindToBusiness(AttachmentAssetCommands.BindToBusinessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AttachmentAsset attachmentAsset = requireAsset(command.assetId());
        AttachmentAsset updatedAsset = attachmentAsset.addBinding(
                command.businessType(),
                command.businessId(),
                command.bindingRole(),
                now()
        );
        if (!updatedAsset.equals(attachmentAsset)) {
            attachmentAssetRepository.save(updatedAsset);
        }
        return updatedAsset.toView();
    }

    public AttachmentAssetView unbindFromBusiness(AttachmentAssetCommands.UnbindFromBusinessCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AttachmentAsset attachmentAsset = requireAsset(command.assetId());
        boolean bindingExists = attachmentAsset.bindings().stream()
                .anyMatch(binding -> binding.id().equals(command.bindingId()));
        if (!bindingExists) {
            throw new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Attachment binding not found");
        }
        AttachmentAsset updatedAsset = attachmentAsset.removeBinding(command.bindingId(), now());
        if (!updatedAsset.equals(attachmentAsset)) {
            attachmentAssetRepository.save(updatedAsset);
        }
        return updatedAsset.toView();
    }

    public AttachmentAssetView updatePreviewStatus(AttachmentAssetCommands.UpdatePreviewStatusCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        AttachmentAsset attachmentAsset = requireAsset(command.assetId());
        AttachmentAsset updatedAsset = attachmentAsset.updatePreviewStatus(command.status(), now());
        if (!updatedAsset.equals(attachmentAsset)) {
            attachmentAssetRepository.save(updatedAsset);
        }
        return updatedAsset.toView();
    }

    public List<AttachmentAssetView> queryByBusiness(String businessType, String businessId) {
        return attachmentAssetRepository.findAllByBusiness(businessType, businessId).stream()
                .map(AttachmentAsset::toView)
                .sorted(Comparator.comparing(AttachmentAssetView::updatedAt).reversed())
                .toList();
    }

    public List<AttachmentAssetView> queryByTenant(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        return attachmentAssetRepository.findAllByTenant(tenantId).stream()
                .map(AttachmentAsset::toView)
                .sorted(Comparator.comparing(AttachmentAssetView::updatedAt).reversed())
                .toList();
    }

    private AttachmentAsset requireAsset(UUID assetId) {
        return attachmentAssetRepository.findById(assetId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Attachment asset not found"
                ));
    }

    private void publishQuotaWarningIfNeeded(
            UUID tenantId,
            UUID attachmentId,
            long previousTotal,
            long currentTotal,
            Instant occurredAt
    ) {
        if (quotaWarningThresholdBytes <= 0) {
            return;
        }
        if (previousTotal < quotaWarningThresholdBytes && currentTotal >= quotaWarningThresholdBytes) {
            domainEventPublisher.publish(AttachmentQuotaWarningEvent.of(
                    tenantId,
                    attachmentId,
                    quotaWarningThresholdBytes,
                    occurredAt
            ));
        }
    }

    private Instant now() {
        return clock.instant();
    }
}
