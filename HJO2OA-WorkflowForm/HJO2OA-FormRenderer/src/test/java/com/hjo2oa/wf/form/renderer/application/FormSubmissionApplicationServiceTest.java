package com.hjo2oa.wf.form.renderer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryApplicationService;
import com.hjo2oa.infra.data.i18n.infrastructure.InMemoryTranslationEntryRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.wf.form.renderer.domain.FieldDefinition;
import com.hjo2oa.wf.form.renderer.domain.FieldType;
import com.hjo2oa.wf.form.renderer.domain.FormMetadataSnapshot;
import com.hjo2oa.wf.form.renderer.domain.FormSubmission;
import com.hjo2oa.wf.form.renderer.domain.FormSubmissionStatus;
import com.hjo2oa.wf.form.renderer.infrastructure.InMemoryFormSubmissionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FormSubmissionApplicationServiceTest {

    private static final UUID METADATA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Instant FIXED_TIME = Instant.parse("2026-04-29T08:00:00Z");

    @Test
    void shouldCreateDraftWithValidationAndAttachmentReferencesIdempotently() {
        FormSubmissionApplicationService service = service();

        FormSubmission draft = service.createDraft(new FormRendererCommands.CreateDraftCommand(
                snapshot(),
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                "start",
                Map.of("amount", "1200", "attachments", List.of("att-1", "att-2")),
                USER_ID,
                "draft-1"
        ));
        FormSubmission replay = service.createDraft(new FormRendererCommands.CreateDraftCommand(
                snapshot(),
                draft.processInstanceId(),
                draft.formDataId(),
                "start",
                Map.of("amount", "800"),
                USER_ID,
                "draft-1"
        ));

        assertThat(draft.status()).isEqualTo(FormSubmissionStatus.DRAFT);
        assertThat(draft.validation().valid()).isFalse();
        assertThat(draft.attachmentIds()).containsExactly("att-1", "att-2");
        assertThat(replay.submissionId()).isEqualTo(draft.submissionId());
        assertThat(replay.formData().get("amount")).isEqualTo("1200");
    }

    @Test
    void shouldRejectInvalidFinalSubmitAndAcceptValidDraft() {
        FormSubmissionApplicationService service = service();
        FormSubmission draft = service.createDraft(new FormRendererCommands.CreateDraftCommand(
                snapshot(),
                null,
                null,
                "start",
                Map.of("amount", "1200"),
                USER_ID,
                "draft-2"
        ));

        assertThatThrownBy(() -> service.submitDraft(new FormRendererCommands.SubmitDraftCommand(
                draft.submissionId(),
                snapshot(),
                Map.of("amount", "1200"),
                "submit-invalid"
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("validation failed");

        FormSubmission submitted = service.submitDraft(new FormRendererCommands.SubmitDraftCommand(
                draft.submissionId(),
                snapshot(),
                Map.of("amount", "800"),
                "submit-valid"
        ));

        assertThat(submitted.status()).isEqualTo(FormSubmissionStatus.SUBMITTED);
        assertThat(submitted.submittedAt()).isEqualTo(FIXED_TIME);
        assertThat(submitted.validation().valid()).isTrue();
    }

    private FormSubmissionApplicationService service() {
        Clock clock = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);
        TranslationEntryApplicationService translationService = new TranslationEntryApplicationService(
                new InMemoryTranslationEntryRepository(),
                clock
        );
        return new FormSubmissionApplicationService(
                new InMemoryFormSubmissionRepository(),
                new FormRendererApplicationService(translationService),
                clock
        );
    }

    private FormMetadataSnapshot snapshot() {
        return new FormMetadataSnapshot(
                METADATA_ID,
                "expense",
                "Expense",
                1,
                List.of(
                        new FieldDefinition(
                                "amount",
                                "Amount",
                                FieldType.NUMBER,
                                true,
                                null,
                                null,
                                false,
                                true,
                                true,
                                null,
                                BigDecimal.ZERO,
                                new BigDecimal("1000"),
                                null,
                                List.of(),
                                null
                        ),
                        new FieldDefinition(
                                "attachments",
                                "Attachments",
                                FieldType.ATTACHMENT,
                                false,
                                null,
                                null,
                                true,
                                true,
                                true,
                                null,
                                null,
                                null,
                                null,
                                List.of(),
                                null
                        )
                ),
                Map.of("type", "vertical"),
                List.of(),
                Map.of(),
                TENANT_ID
        );
    }
}
