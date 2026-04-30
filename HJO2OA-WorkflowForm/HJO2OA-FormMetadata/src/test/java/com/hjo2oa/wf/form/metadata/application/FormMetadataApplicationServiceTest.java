package com.hjo2oa.wf.form.metadata.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormFieldType;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataDetailView;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import com.hjo2oa.wf.form.metadata.infrastructure.InMemoryFormMetadataRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FormMetadataApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-27T01:00:00Z");
    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void shouldCreatePublishAndDeriveVersion() {
        FormMetadataApplicationService service = service();

        FormMetadataDetailView created = service.create(createCommand("leave.request"));
        FormMetadataDetailView published = service.publish(created.id());
        FormMetadataDetailView versionTwo = service.deriveNewVersion(published.id());

        assertThat(created.status()).isEqualTo(FormMetadataStatus.DRAFT);
        assertThat(published.status()).isEqualTo(FormMetadataStatus.PUBLISHED);
        assertThat(published.publishedAt()).isEqualTo(FIXED_TIME);
        assertThat(versionTwo.version()).isEqualTo(2);
        assertThat(versionTwo.status()).isEqualTo(FormMetadataStatus.DRAFT);
        assertThat(service.latestRenderSchema(TENANT_ID, "leave.request").version()).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicateFieldCode() {
        FormMetadataApplicationService service = service();
        FormFieldDefinition duplicated = textField("title");

        FormMetadataCommands.SaveFormMetadataCommand command = new FormMetadataCommands.SaveFormMetadataCommand(
                "duplicate.fields",
                "Duplicate Fields",
                null,
                List.of(duplicated, duplicated),
                layout(),
                null,
                null,
                TENANT_ID
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("duplicate fieldCode");
    }

    @Test
    void shouldRejectPermissionMapThatReferencesUnknownField() {
        FormMetadataApplicationService service = service();
        JsonNode badPermissionMap = JsonNodeFactory.instance.objectNode()
                .set("start", JsonNodeFactory.instance.objectNode()
                        .set("missingField", JsonNodeFactory.instance.objectNode()
                                .put("visible", true)));

        FormMetadataCommands.SaveFormMetadataCommand command = new FormMetadataCommands.SaveFormMetadataCommand(
                "bad.permission",
                "Bad Permission",
                null,
                List.of(textField("title")),
                layout(),
                null,
                badPermissionMap,
                TENANT_ID
        );

        assertThatThrownBy(() -> service.create(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("permission references unknown fieldCode");
    }

    @Test
    void shouldReportFormMetadataValidationCountsAndIssues() {
        FormMetadataApplicationService service = service();
        FormMetadataDetailView created = service.create(createCommand("validation.report"));

        var report = service.validate(created.id());

        assertThat(report.valid()).isTrue();
        assertThat(report.fieldCount()).isEqualTo(2);
        assertThat(report.permissionNodeCount()).isEqualTo(1);
        assertThat(report.issues()).isEmpty();
    }

    @Test
    void shouldRejectUpdatingPublishedMetadata() {
        FormMetadataApplicationService service = service();
        FormMetadataDetailView created = service.create(createCommand("expense.claim"));
        service.publish(created.id());

        FormMetadataCommands.UpdateFormMetadataCommand command = new FormMetadataCommands.UpdateFormMetadataCommand(
                created.id(),
                "Expense Claim Updated",
                null,
                List.of(textField("amount")),
                layout(),
                null,
                null
        );

        assertThatThrownBy(() -> service.update(command))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Only draft form metadata can be updated");
    }

    private FormMetadataApplicationService service() {
        return new FormMetadataApplicationService(
                new InMemoryFormMetadataRepository(),
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private FormMetadataCommands.SaveFormMetadataCommand createCommand(String code) {
        return new FormMetadataCommands.SaveFormMetadataCommand(
                code,
                "Leave Request",
                "form.leave.name",
                List.of(textField("title"), selectField("leaveType")),
                layout(),
                null,
                permissionMap(),
                TENANT_ID
        );
    }

    private FormFieldDefinition textField(String code) {
        return new FormFieldDefinition(
                code,
                code + " name",
                "form.field." + code,
                FormFieldType.TEXT,
                true,
                null,
                null,
                false,
                true,
                true,
                128,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    private FormFieldDefinition selectField(String code) {
        return new FormFieldDefinition(
                code,
                code + " name",
                "form.field." + code,
                FormFieldType.SELECT,
                true,
                null,
                "leave.type",
                false,
                true,
                true,
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    private JsonNode layout() {
        return JsonNodeFactory.instance.objectNode().put("type", "grid");
    }

    private JsonNode permissionMap() {
        return JsonNodeFactory.instance.objectNode()
                .set("start", JsonNodeFactory.instance.objectNode()
                        .set("title", JsonNodeFactory.instance.objectNode()
                                .put("visible", true)
                                .put("editable", true)
                                .put("required", true)));
    }
}
