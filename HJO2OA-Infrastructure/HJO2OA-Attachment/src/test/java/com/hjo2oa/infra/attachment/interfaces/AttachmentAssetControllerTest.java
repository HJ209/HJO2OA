package com.hjo2oa.infra.attachment.interfaces;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hjo2oa.infra.attachment.application.AttachmentAssetApplicationService;
import com.hjo2oa.infra.attachment.application.AttachmentAssetCommands;
import com.hjo2oa.infra.attachment.domain.BindingRole;
import com.hjo2oa.infra.attachment.domain.StorageProvider;
import com.hjo2oa.infra.attachment.infrastructure.InMemoryAttachmentAssetRepository;
import com.hjo2oa.shared.web.ResponseMetaFactory;
import com.hjo2oa.shared.web.SharedGlobalExceptionHandler;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AttachmentAssetControllerTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-24T07:30:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CREATOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void shouldCreateAttachmentUsingSharedWebContract() throws Exception {
        MockMvc mockMvc = buildMockMvc();

        mockMvc.perform(post("/api/v1/infra/attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-attachment-create-1")
                        .content("""
                                {
                                  "storageKey":"tenant-1/attachments/create-1",
                                  "originalFilename":"proposal.pdf",
                                  "contentType":"application/pdf",
                                  "sizeBytes":88,
                                  "checksum":"checksum-create-1",
                                  "storageProvider":"MINIO",
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "createdBy":"22222222-2222-2222-2222-222222222222"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.originalFilename").value("proposal.pdf"))
                .andExpect(jsonPath("$.data.storageProvider").value("MINIO"))
                .andExpect(jsonPath("$.data.latestVersionNo").value(1))
                .andExpect(jsonPath("$.meta.requestId").value("req-attachment-create-1"));
    }

    @Test
    void shouldAddVersionAndQueryByBusiness() throws Exception {
        AttachmentAssetApplicationService applicationService = applicationService();
        var created = applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/attachments/query-1",
                "manual.pdf",
                "application/pdf",
                70L,
                "checksum-query-v1",
                StorageProvider.S3,
                TENANT_ID,
                CREATOR_ID
        ));
        applicationService.bindToBusiness(new AttachmentAssetCommands.BindToBusinessCommand(
                created.id(),
                "DOC",
                "doc-001",
                BindingRole.PRIMARY
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/attachments/" + created.id() + "/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageKey":"tenant-1/attachments/query-1-v2",
                                  "checksum":"checksum-query-v2",
                                  "sizeBytes":91,
                                  "createdBy":"22222222-2222-2222-2222-222222222222"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.latestVersionNo").value(2))
                .andExpect(jsonPath("$.data.versions.length()").value(2));

        mockMvc.perform(get("/api/v1/infra/attachments/business/DOC/doc-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(created.id().toString()))
                .andExpect(jsonPath("$.data[0].bindings[0].bindingRole").value("PRIMARY"));

        mockMvc.perform(get("/api/v1/infra/attachments/" + created.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originalFilename").value("manual.pdf"))
                .andExpect(jsonPath("$.data.latestVersionNo").value(2));
    }

    @Test
    void shouldUpdatePreviewStatusUnbindAndQueryByTenant() throws Exception {
        AttachmentAssetApplicationService applicationService = applicationService();
        var created = applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/attachments/query-2",
                "cover.png",
                "image/png",
                30L,
                "checksum-cover-v1",
                StorageProvider.LOCAL,
                TENANT_ID,
                CREATOR_ID
        ));
        var bound = applicationService.bindToBusiness(new AttachmentAssetCommands.BindToBusinessCommand(
                created.id(),
                "ARTICLE",
                "article-001",
                BindingRole.COVER
        ));
        UUID bindingId = bound.bindings().get(0).id();
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(put("/api/v1/infra/attachments/" + created.id() + "/preview-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(ResponseMetaFactory.REQUEST_ID_HEADER, "req-attachment-preview-1")
                        .content("""
                                {
                                  "status":"READY"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewStatus").value("READY"))
                .andExpect(jsonPath("$.meta.requestId").value("req-attachment-preview-1"));

        mockMvc.perform(delete("/api/v1/infra/attachments/" + created.id() + "/bindings/" + bindingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bindings[0].active").value(false));

        mockMvc.perform(get("/api/v1/infra/attachments")
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tenantId").value(TENANT_ID.toString()));

        mockMvc.perform(get("/api/v1/infra/attachments/business/ARTICLE/article-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldRejectDuplicateStorageKeyAsConflict() throws Exception {
        AttachmentAssetApplicationService applicationService = applicationService();
        applicationService.createAsset(new AttachmentAssetCommands.CreateAssetCommand(
                "tenant-1/attachments/duplicate",
                "dup-1.txt",
                "text/plain",
                10L,
                "checksum-dup-1",
                StorageProvider.LOCAL,
                TENANT_ID,
                CREATOR_ID
        ));
        MockMvc mockMvc = buildMockMvc(applicationService);

        mockMvc.perform(post("/api/v1/infra/attachments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "storageKey":"tenant-1/attachments/duplicate",
                                  "originalFilename":"dup-2.txt",
                                  "contentType":"text/plain",
                                  "sizeBytes":12,
                                  "checksum":"checksum-dup-2",
                                  "storageProvider":"LOCAL",
                                  "tenantId":"11111111-1111-1111-1111-111111111111",
                                  "createdBy":"22222222-2222-2222-2222-222222222222"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    private MockMvc buildMockMvc() {
        return buildMockMvc(applicationService());
    }

    private MockMvc buildMockMvc(AttachmentAssetApplicationService applicationService) {
        ResponseMetaFactory responseMetaFactory = new ResponseMetaFactory();
        AttachmentAssetDtoMapper dtoMapper = new AttachmentAssetDtoMapper();
        return MockMvcBuilders.standaloneSetup(
                        new AttachmentAssetController(applicationService, dtoMapper, responseMetaFactory)
                )
                .setControllerAdvice(new SharedGlobalExceptionHandler(responseMetaFactory))
                .build();
    }

    private AttachmentAssetApplicationService applicationService() {
        return new AttachmentAssetApplicationService(
                new InMemoryAttachmentAssetRepository(),
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
