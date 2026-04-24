package com.hjo2oa.infra.attachment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.infra.attachment.domain.AttachmentCreatedEvent;
import com.hjo2oa.infra.attachment.domain.AttachmentQuotaWarningEvent;
import com.hjo2oa.infra.attachment.domain.AttachmentVersionCreatedEvent;
import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.PreviewStatus;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import com.hjo2oa.infra.attachment.infrastructure.InMemoryAttachmentAssetRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AttachmentAssetApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T07:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CREATOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldPublishCreatedVersionAndQuotaEvents() {
        List<DomainEvent> publishedEvents = new ArrayList<>();
        AttachmentAssetApplicationService applicationService = new AttachmentAssetApplicationService(
                new InMemoryAttachmentAssetRepository(),
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                100L
        );

        var created = applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/contracts/file-a-v1",
                "contract.pdf",
                "application/pdf",
                60L,
                "checksum-v1",
                StorageProvider.MINIO,
                TENANT_ID,
                CREATOR_ID
        ));
        var updated = applicationService.addVersion(new AttachmentAssetCommands.AddVersionCommand(
                created.id(),
                "tenant-1/contracts/file-a-v2",
                "checksum-v2",
                120L,
                CREATOR_ID
        ));

        assertThat(created.latestVersionNo()).isEqualTo(1);
        assertThat(updated.latestVersionNo()).isEqualTo(2);
        assertThat(updated.storageKey()).isEqualTo("tenant-1/contracts/file-a-v2");
        assertThat(updated.versions()).hasSize(2);
        assertThat(publishedEvents).hasSize(3);
        assertThat(publishedEvents.get(0)).isInstanceOf(AttachmentCreatedEvent.class);
        assertThat(publishedEvents.get(1)).isInstanceOf(AttachmentVersionCreatedEvent.class);
        assertThat(publishedEvents.get(2)).isInstanceOf(AttachmentQuotaWarningEvent.class);

        AttachmentCreatedEvent createdEvent = (AttachmentCreatedEvent) publishedEvents.get(0);
        assertThat(createdEvent.attachmentId()).isEqualTo(created.id());
        assertThat(createdEvent.fileName()).isEqualTo("contract.pdf");
        assertThat(createdEvent.storageProvider()).isEqualTo(StorageProvider.MINIO);

        AttachmentVersionCreatedEvent versionCreatedEvent = (AttachmentVersionCreatedEvent) publishedEvents.get(1);
        assertThat(versionCreatedEvent.attachmentId()).isEqualTo(created.id());
        assertThat(versionCreatedEvent.versionNo()).isEqualTo(2);

        AttachmentQuotaWarningEvent quotaWarningEvent = (AttachmentQuotaWarningEvent) publishedEvents.get(2);
        assertThat(quotaWarningEvent.tenantId()).isEqualTo(TENANT_ID.toString());
        assertThat(quotaWarningEvent.attachmentId()).isEqualTo(created.id());
        assertThat(quotaWarningEvent.threshold()).isEqualTo(100L);
    }

    @Test
    void shouldBindUnbindUpdatePreviewAndQuery() {
        AttachmentAssetApplicationService applicationService = new AttachmentAssetApplicationService(
                new InMemoryAttachmentAssetRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                Long.MAX_VALUE
        );

        var created = applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/contracts/file-b-v1",
                "spec.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                32L,
                "checksum-docx-v1",
                StorageProvider.S3,
                TENANT_ID,
                CREATOR_ID
        ));
        var bound = applicationService.bindToBusiness(new AttachmentAssetCommands.BindToBusinessCommand(
                created.id(),
                "CONTRACT",
                "contract-001",
                BindingRole.ATTACHMENT
        ));
        var previewUpdated = applicationService.updatePreviewStatus(new AttachmentAssetCommands.UpdatePreviewStatusCommand(
                created.id(),
                PreviewStatus.READY
        ));

        assertThat(bound.bindings()).hasSize(1);
        assertThat(previewUpdated.previewStatus()).isEqualTo(PreviewStatus.READY);
        assertThat(applicationService.queryByBusiness("CONTRACT", "contract-001")).hasSize(1);
        assertThat(applicationService.queryByTenant(TENANT_ID)).hasSize(1);

        UUID bindingId = bound.bindings().get(0).id();
        var unbound = applicationService.unbindFromBusiness(
                new AttachmentAssetCommands.UnbindFromBusinessCommand(created.id(), bindingId)
        );

        assertThat(unbound.bindings()).singleElement().extracting("active").isEqualTo(false);
        assertThat(applicationService.queryByBusiness("CONTRACT", "contract-001")).isEmpty();
    }

    @Test
    void shouldRejectDuplicateStorageKey() {
        AttachmentAssetApplicationService applicationService = new AttachmentAssetApplicationService(
                new InMemoryAttachmentAssetRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC),
                Long.MAX_VALUE
        );

        applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/contracts/file-c-v1",
                "duplicate.txt",
                "text/plain",
                12L,
                "checksum-1",
                StorageProvider.LOCAL,
                TENANT_ID,
                CREATOR_ID
        ));

        assertThatThrownBy(() -> applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/contracts/file-c-v1",
                "duplicate-2.txt",
                "text/plain",
                15L,
                "checksum-2",
                StorageProvider.LOCAL,
                TENANT_ID,
                CREATOR_ID
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Attachment storage key already exists");
    }
}
