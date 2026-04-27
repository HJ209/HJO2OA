package com.hjo2oa.wf.form.renderer.application;

import com.hjo2oa.infra.data.i18n.application.TranslationEntryApplicationService;
import com.hjo2oa.infra.data.i18n.application.TranslationEntryCommands;
import com.hjo2oa.infra.data.i18n.domain.TranslationResolutionView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.form.renderer.domain.FieldDefinition;
import com.hjo2oa.wf.form.renderer.domain.FieldPermission;
import com.hjo2oa.wf.form.renderer.domain.FieldType;
import com.hjo2oa.wf.form.renderer.domain.FormMetadataSnapshot;
import com.hjo2oa.wf.form.renderer.domain.FormValidationResultView;
import com.hjo2oa.wf.form.renderer.domain.RenderedFieldView;
import com.hjo2oa.wf.form.renderer.domain.RenderedFormView;
import com.hjo2oa.wf.form.renderer.domain.ValidationErrorView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.springframework.stereotype.Service;

@Service
public class FormRendererApplicationService {

    private static final String FORM_METADATA_ENTITY_TYPE = "form_metadata";

    private final TranslationEntryApplicationService translationService;

    public FormRendererApplicationService(TranslationEntryApplicationService translationService) {
        this.translationService = Objects.requireNonNull(translationService, "translationService must not be null");
    }

    public RenderedFormView renderForm(FormRendererCommands.RenderCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FormMetadataSnapshot snapshot = requireSnapshot(command.metadataSnapshot());
        String locale = normalizeLocale(command.locale());
        Map<String, Object> formData = command.formData() == null ? Map.of() : command.formData();
        FormValidationResultView validation = command.validateData()
                ? validateForm(new FormRendererCommands.ValidateCommand(snapshot, command.nodeId(), formData))
                : new FormValidationResultView(true, List.of());

        return new RenderedFormView(
                snapshot.metadataId(),
                snapshot.code(),
                snapshot.name(),
                resolveDisplayName(snapshot, locale, command.fallbackLocale()),
                snapshot.version(),
                normalizeNullableText(command.nodeId()),
                locale,
                command.processInstanceId(),
                command.formDataId(),
                snapshot.layout(),
                snapshot.fields().stream()
                        .map(field -> renderField(snapshot, field, command.nodeId(), locale, command.fallbackLocale(),
                                formData))
                        .toList(),
                validation
        );
    }

    public FormValidationResultView validateForm(FormRendererCommands.ValidateCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        FormMetadataSnapshot snapshot = requireSnapshot(command.metadataSnapshot());
        Map<String, Object> formData = command.formData() == null ? Map.of() : command.formData();
        List<ValidationErrorView> errors = new ArrayList<>();
        for (FieldDefinition field : snapshot.fields()) {
            validateField(snapshot, field, command.nodeId(), formData.get(field.fieldCode()), field.fieldCode(), errors);
        }
        return new FormValidationResultView(errors.isEmpty(), errors);
    }

    private RenderedFieldView renderField(
            FormMetadataSnapshot snapshot,
            FieldDefinition field,
            String nodeId,
            String locale,
            String fallbackLocale,
            Map<String, Object> formData
    ) {
        requireField(field);
        FieldPermission permission = permissionFor(snapshot, nodeId, field.fieldCode());
        boolean visible = permission.resolveVisible(field.isVisibleByDefault());
        boolean editable = permission.resolveEditable(field.isEditableByDefault());
        boolean required = permission.resolveRequired(field.isRequiredByDefault());
        Object value = formData.containsKey(field.fieldCode()) ? formData.get(field.fieldCode()) : field.defaultValue();
        return new RenderedFieldView(
                field.fieldCode(),
                field.fieldName(),
                resolveFieldDisplayName(snapshot, field, locale, fallbackLocale),
                field.fieldType(),
                value,
                field.dictionaryCode(),
                field.isMultiValue(),
                visible,
                editable,
                required,
                field.maxLength(),
                field.min(),
                field.max(),
                field.pattern(),
                field.childFields().stream()
                        .map(child -> renderChildField(snapshot, child, nodeId, locale, fallbackLocale))
                        .toList(),
                field.linkageRules()
        );
    }

    private RenderedFieldView renderChildField(
            FormMetadataSnapshot snapshot,
            FieldDefinition field,
            String nodeId,
            String locale,
            String fallbackLocale
    ) {
        requireField(field);
        FieldPermission permission = permissionFor(snapshot, nodeId, field.fieldCode());
        return new RenderedFieldView(
                field.fieldCode(),
                field.fieldName(),
                resolveFieldDisplayName(snapshot, field, locale, fallbackLocale),
                field.fieldType(),
                field.defaultValue(),
                field.dictionaryCode(),
                field.isMultiValue(),
                permission.resolveVisible(field.isVisibleByDefault()),
                permission.resolveEditable(field.isEditableByDefault()),
                permission.resolveRequired(field.isRequiredByDefault()),
                field.maxLength(),
                field.min(),
                field.max(),
                field.pattern(),
                field.childFields().stream()
                        .map(child -> renderChildField(snapshot, child, nodeId, locale, fallbackLocale))
                        .toList(),
                field.linkageRules()
        );
    }

    private void validateField(
            FormMetadataSnapshot snapshot,
            FieldDefinition field,
            String nodeId,
            Object value,
            String path,
            List<ValidationErrorView> errors
    ) {
        requireField(field);
        FieldPermission permission = permissionFor(snapshot, nodeId, field.fieldCode());
        if (!permission.resolveVisible(field.isVisibleByDefault())) {
            return;
        }
        if (permission.resolveRequired(field.isRequiredByDefault()) && isEmptyValue(value)) {
            errors.add(new ValidationErrorView(path, "field is required"));
            return;
        }
        if (isEmptyValue(value)) {
            return;
        }
        validateByType(field, value, path, errors);
        validateLengthAndRange(field, value, path, errors);
        validatePattern(field, value, path, errors);
    }

    @SuppressWarnings("unchecked")
    private void validateByType(
            FieldDefinition field,
            Object value,
            String path,
            List<ValidationErrorView> errors
    ) {
        FieldType type = field.fieldType();
        if (type == FieldType.NUMBER && toBigDecimal(value) == null) {
            errors.add(new ValidationErrorView(path, "field must be a number"));
        } else if (type == FieldType.DATE && !canParseDate(value)) {
            errors.add(new ValidationErrorView(path, "field must be an ISO date"));
        } else if (type == FieldType.DATETIME && !canParseDateTime(value)) {
            errors.add(new ValidationErrorView(path, "field must be an ISO datetime"));
        } else if ((type == FieldType.MULTI_SELECT || field.isMultiValue()) && !(value instanceof Collection<?>)) {
            errors.add(new ValidationErrorView(path, "field must contain multiple values"));
        } else if (type == FieldType.TABLE && value instanceof List<?> rows) {
            validateTableRows(field, rows, path, errors);
        } else if (type == FieldType.TABLE && !(value instanceof List<?>)) {
            errors.add(new ValidationErrorView(path, "field must be a table row list"));
        }
    }

    private void validateTableRows(
            FieldDefinition tableField,
            List<?> rows,
            String path,
            List<ValidationErrorView> errors
    ) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            Object row = rows.get(rowIndex);
            if (!(row instanceof Map<?, ?> rowData)) {
                errors.add(new ValidationErrorView(path + "[" + rowIndex + "]", "table row must be an object"));
                continue;
            }
            for (FieldDefinition child : tableField.childFields()) {
                Object childValue = rowData.get(child.fieldCode());
                validateField(new FormMetadataSnapshot(null, null, null, null, List.of(), null, List.of(),
                                Map.of(), null),
                        child, null, childValue, path + "[" + rowIndex + "]." + child.fieldCode(), errors);
            }
        }
    }

    private void validateLengthAndRange(
            FieldDefinition field,
            Object value,
            String path,
            List<ValidationErrorView> errors
    ) {
        if (field.maxLength() != null && String.valueOf(value).length() > field.maxLength()) {
            errors.add(new ValidationErrorView(path, "field length must be less than or equal to "
                    + field.maxLength()));
        }
        BigDecimal numericValue = toBigDecimal(value);
        if (numericValue == null) {
            return;
        }
        if (field.min() != null && numericValue.compareTo(field.min()) < 0) {
            errors.add(new ValidationErrorView(path, "field must be greater than or equal to " + field.min()));
        }
        if (field.max() != null && numericValue.compareTo(field.max()) > 0) {
            errors.add(new ValidationErrorView(path, "field must be less than or equal to " + field.max()));
        }
    }

    private void validatePattern(
            FieldDefinition field,
            Object value,
            String path,
            List<ValidationErrorView> errors
    ) {
        String pattern = normalizeNullableText(field.pattern());
        if (pattern == null) {
            return;
        }
        try {
            if (!Pattern.compile(pattern).matcher(String.valueOf(value)).matches()) {
                errors.add(new ValidationErrorView(path, "field format is invalid"));
            }
        } catch (PatternSyntaxException ex) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "invalid field pattern for " + field.fieldCode(),
                    ex
            );
        }
    }

    private String resolveDisplayName(FormMetadataSnapshot snapshot, String locale, String fallbackLocale) {
        return resolveTranslation(snapshot, "name", locale, fallbackLocale, snapshot.name());
    }

    private String resolveFieldDisplayName(
            FormMetadataSnapshot snapshot,
            FieldDefinition field,
            String locale,
            String fallbackLocale
    ) {
        return resolveTranslation(snapshot, "field." + field.fieldCode() + ".name",
                locale, fallbackLocale, field.fieldName());
    }

    private String resolveTranslation(
            FormMetadataSnapshot snapshot,
            String fieldName,
            String locale,
            String fallbackLocale,
            String originalValue
    ) {
        TranslationResolutionView resolution = translationService.resolveTranslation(
                new TranslationEntryCommands.ResolveCommand(
                        FORM_METADATA_ENTITY_TYPE,
                        snapshot.metadataId().toString(),
                        fieldName,
                        locale,
                        snapshot.tenantId(),
                        fallbackLocale,
                        originalValue
                )
        );
        return resolution.resolvedValue();
    }

    private FieldPermission permissionFor(FormMetadataSnapshot snapshot, String nodeId, String fieldCode) {
        String normalizedNodeId = normalizeNullableText(nodeId);
        if (normalizedNodeId == null) {
            return new FieldPermission(null, null, null);
        }
        Map<String, FieldPermission> nodePermissions = snapshot.fieldPermissionMap().get(normalizedNodeId);
        if (nodePermissions == null) {
            return new FieldPermission(null, null, null);
        }
        return nodePermissions.getOrDefault(fieldCode, new FieldPermission(null, null, null));
    }

    private FormMetadataSnapshot requireSnapshot(FormMetadataSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "metadataSnapshot must not be null");
        Objects.requireNonNull(snapshot.metadataId(), "metadataId must not be null");
        Objects.requireNonNull(snapshot.tenantId(), "tenantId must not be null");
        requireText(snapshot.code(), "metadata code");
        requireText(snapshot.name(), "metadata name");
        if (snapshot.version() == null || snapshot.version() < 1) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "metadata version must be greater than or equal to 1"
            );
        }
        snapshot.fields().forEach(this::requireField);
        return snapshot;
    }

    private void requireField(FieldDefinition field) {
        Objects.requireNonNull(field, "field must not be null");
        requireText(field.fieldCode(), "fieldCode");
        requireText(field.fieldName(), "fieldName");
        Objects.requireNonNull(field.fieldType(), "fieldType must not be null");
    }

    private String requireText(String value, String fieldName) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    fieldName + " must not be blank"
            );
        }
        return normalized;
    }

    private String normalizeLocale(String value) {
        String normalized = requireText(value, "locale").replace('_', '-');
        String[] segments = normalized.split("-");
        if (segments.length == 1) {
            return segments[0].toLowerCase(Locale.ROOT);
        }
        StringBuilder builder = new StringBuilder(segments[0].toLowerCase(Locale.ROOT));
        for (int index = 1; index < segments.length; index++) {
            builder.append('-');
            builder.append(index == 1 ? segments[index].toUpperCase(Locale.ROOT) : segments[index]);
        }
        return builder.toString();
    }

    private String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean canParseDate(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            LocalDate.parse(text);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private boolean canParseDateTime(Object value) {
        if (!(value instanceof String text)) {
            return false;
        }
        try {
            OffsetDateTime.parse(text);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }
}
