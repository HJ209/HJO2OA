package com.hjo2oa.infra.attachment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.PreviewStatus;
import com.hjo2oa.infra.attachment.infrastructure.InMemoryAttachmentAccessAuditRepository;
import com.hjo2oa.infra.attachment.infrastructure.InMemoryAttachmentAssetRepository;
import com.hjo2oa.infra.attachment.infrastructure.LocalAttachmentStorageAdapter;
import com.hjo2oa.infra.attachment.infrastructure.NoopAttachmentVirusScanner;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.tenant.TenantContextHolder;
import com.hjo2oa.shared.tenant.TenantRequestContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AttachmentFileApplicationServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID CREATOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-04-29T08:30:00Z"), ZoneOffset.UTC);

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadDownloadVersionPreviewAuditAndRejectCrossTenantAccess() {
        Fixture fixture = fixture();
        try (var ignored = TenantContextHolder.bind(TenantRequestContext.builder().tenantId(TENANT_ID).build())) {
            var uploaded = fixture.fileService().upload(new AttachmentFileApplicationService.UploadCommand(
                    "hello.txt",
                    "text/plain",
                    11L,
                    new ByteArrayInputStream("hello world".getBytes(StandardCharsets.UTF_8)),
                    TENANT_ID,
                    CREATOR_ID,
                    "DOC",
                    "doc-001",
                    BindingRole.ATTACHMENT,
                    "127.0.0.1"
            ));

            assertThat(uploaded.previewStatus()).isEqualTo(PreviewStatus.READY);
            assertThat(uploaded.bindings()).singleElement().extracting("businessId").isEqualTo("doc-001");
            assertThat(uploaded.checksum()).hasSize(64);

            var downloaded = fixture.fileService().download(
                    uploaded.id(),
                    null,
                    "DOC",
                    "doc-001",
                    CREATOR_ID,
                    "127.0.0.1"
            );
            assertThat(downloaded.resource().exists()).isTrue();
            assertThat(downloaded.sizeBytes()).isEqualTo(11L);

            var versioned = fixture.fileService().uploadVersion(new AttachmentFileApplicationService.UploadVersionCommand(
                    uploaded.id(),
                    "hello-v2.txt",
                    "text/plain",
                    12L,
                    new ByteArrayInputStream("hello v2 data".getBytes(StandardCharsets.UTF_8)),
                    CREATOR_ID,
                    "127.0.0.1"
            ));
            assertThat(versioned.latestVersionNo()).isEqualTo(2);

            var preview = fixture.fileService().preview(uploaded.id(), "DOC", "doc-001", CREATOR_ID, "127.0.0.1");
            assertThat(preview.previewAvailable()).isTrue();
            assertThat(fixture.fileService().audit(uploaded.id()))
                    .extracting(AttachmentAccessAuditRecord::action)
                    .contains("UPLOAD", "DOWNLOAD", "UPLOAD_VERSION", "PREVIEW");

            try (var other = TenantContextHolder.bind(TenantRequestContext.builder().tenantId(OTHER_TENANT_ID).build())) {
                assertThatThrownBy(() -> fixture.fileService().download(
                        uploaded.id(),
                        null,
                        "DOC",
                        "doc-001",
                        CREATOR_ID,
                        "127.0.0.1"
                ))
                        .isInstanceOf(BizException.class)
                        .hasMessageContaining("another tenant");
            }
        }
    }

    private Fixture fixture() {
        InMemoryAttachmentAssetRepository assetRepository = new InMemoryAttachmentAssetRepository();
        AttachmentAssetApplicationService assetService = new AttachmentAssetApplicationService(
                assetRepository,
                event -> {
                },
                CLOCK
        );
        AttachmentStorageAdapterRegistry storageRegistry = new AttachmentStorageAdapterRegistry(List.of(
                new LocalAttachmentStorageAdapter(tempDir.toString())
        ));
        AttachmentAccessAuditService auditService = new AttachmentAccessAuditService(
                new InMemoryAttachmentAccessAuditRepository(),
                CLOCK
        );
        AttachmentFileApplicationService fileService = new AttachmentFileApplicationService(
                assetService,
                assetRepository,
                storageRegistry,
                new AttachmentUploadPolicy(1024L, List.of("text/plain")),
                new NoopAttachmentVirusScanner(),
                new AttachmentAuthorizationService(),
                auditService,
                CLOCK
        );
        return new Fixture(fileService);
    }

    private record Fixture(
            AttachmentFileApplicationService fileService
    ) {
    }
}
