package com.hjo2oa.wf.form.renderer.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryApplicationService;
import com.hjo2oa.infra.data.i18n.infrastructure.InMemoryTranslationEntryRepository;
import com.hjo2oa.wf.form.renderer.domain.FieldDefinition;
import com.hjo2oa.wf.form.renderer.domain.FieldPermission;
import com.hjo2oa.wf.form.renderer.domain.FieldType;
import com.hjo2oa.wf.form.renderer.domain.FormMetadataSnapshot;
import com.hjo2oa.wf.form.renderer.domain.RenderedFormView;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FormRendererApplicationServiceTest {

    private static final UUID METADATA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldRenderFormWithNodePermissionAndDataI18nTranslation() {
        TranslationEntryApplicationService translationService = translationService();
        translationService.createTranslation(
                "form_metadata",
                METADATA_ID.toString(),
                "field.amount.name",
                "en-US",
                "Amount",
                TENANT_ID
        );
        FormRendererApplicationService service = new FormRendererApplicationService(translationService);

        RenderedFormView view = service.renderForm(new FormRendererCommands.RenderCommand(
                metadataSnapshot(),
                UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
                UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"),
                "approve",
                "en-US",
                "zh-CN",
                Map.of("amount", "300", "subject", "REQ-2026"),
                true
        ));

        assertThat(view.fields()).hasSize(2);
        assertThat(view.fields().get(0).displayName()).isEqualTo("Amount");
        assertThat(view.fields().get(0).editable()).isFalse();
        assertThat(view.fields().get(0).required()).isTrue();
        assertThat(view.fields().get(0).value()).isEqualTo("300");
        assertThat(view.validation().valid()).isTrue();
    }

    @Test
    void shouldValidateRequiredRangeAndPatternRules() {
        FormRendererApplicationService service = new FormRendererApplicationService(translationService());

        var result = service.validateForm(new FormRendererCommands.ValidateCommand(
                metadataSnapshot(),
                "approve",
                Map.of("amount", "1200", "subject", "bad subject")
        ));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .extracting("fieldCode")
                .containsExactlyInAnyOrder("amount", "subject");
    }

    private TranslationEntryApplicationService translationService() {
        return new TranslationEntryApplicationService(
                new InMemoryTranslationEntryRepository(),
                Clock.fixed(Instant.parse("2026-04-24T06:30:00Z"), ZoneOffset.UTC)
        );
    }

    private FormMetadataSnapshot metadataSnapshot() {
        FieldDefinition amount = new FieldDefinition(
                "amount",
                "金额",
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
        );
        FieldDefinition subject = new FieldDefinition(
                "subject",
                "标题",
                FieldType.TEXT,
                true,
                null,
                null,
                false,
                true,
                true,
                20,
                null,
                null,
                "^REQ-.+",
                List.of(),
                null
        );
        return new FormMetadataSnapshot(
                METADATA_ID,
                "expense",
                "报销单",
                3,
                List.of(amount, subject),
                Map.of("type", "vertical"),
                List.of(),
                Map.of("approve", Map.of("amount", new FieldPermission(null, false, true))),
                TENANT_ID
        );
    }
}
