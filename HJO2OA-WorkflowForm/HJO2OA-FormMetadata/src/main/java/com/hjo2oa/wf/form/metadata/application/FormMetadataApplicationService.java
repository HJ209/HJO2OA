package com.hjo2oa.wf.form.metadata.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormMetadata;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataDetailView;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataRepository;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataValidationIssue;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataValidationReport;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataView;
import com.hjo2oa.wf.form.metadata.domain.FormRenderSchemaView;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class FormMetadataApplicationService {

    private static final Comparator<FormMetadata> UPDATED_DESC = Comparator
            .comparing(FormMetadata::updatedAt)
            .reversed()
            .thenComparing(FormMetadata::code)
            .thenComparing(FormMetadata::version, Comparator.reverseOrder());

    private static final Comparator<FormMetadata> VERSION_DESC = Comparator
            .comparing(FormMetadata::version)
            .reversed();

    private final FormMetadataRepository repository;
    private final Clock clock;
    @Autowired
    public FormMetadataApplicationService(FormMetadataRepository repository) {
        this(repository, Clock.systemUTC());
    }
    public FormMetadataApplicationService(FormMetadataRepository repository, Clock clock) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public FormMetadataDetailView create(FormMetadataCommands.SaveFormMetadataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        ensureVersionAvailable(command.tenantId(), command.code(), 1);
        validateMetadata(command.fieldSchema(), command.layout(), command.validations(), command.fieldPermissionMap());
        FormMetadata metadata = FormMetadata.create(
                UUID.randomUUID(),
                command.code(),
                command.name(),
                command.nameI18nKey(),
                command.fieldSchema(),
                command.layout(),
                command.validations(),
                command.fieldPermissionMap(),
                command.tenantId(),
                now()
        );
        return repository.save(metadata).toDetailView();
    }

    public FormMetadataDetailView update(FormMetadataCommands.UpdateFormMetadataCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        validateMetadata(command.fieldSchema(), command.layout(), command.validations(), command.fieldPermissionMap());
        FormMetadata metadata = loadRequired(command.metadataId());
        try {
            return repository.save(metadata.updateDraft(
                    command.name(),
                    command.nameI18nKey(),
                    command.fieldSchema(),
                    command.layout(),
                    command.validations(),
                    command.fieldPermissionMap(),
                    now()
            )).toDetailView();
        } catch (IllegalStateException ex) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Only draft form metadata can be updated", ex);
        }
    }

    public FormMetadataDetailView publish(UUID metadataId) {
        FormMetadata metadata = loadRequired(metadataId);
        validateMetadata(metadata.fieldSchema(), metadata.layout(), metadata.validations(), metadata.fieldPermissionMap());
        try {
            return repository.save(metadata.publish(now())).toDetailView();
        } catch (IllegalStateException ex) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Only draft form metadata can be published", ex);
        }
    }

    public FormMetadataDetailView deprecate(UUID metadataId) {
        FormMetadata metadata = loadRequired(metadataId);
        try {
            return repository.save(metadata.deprecate(now())).toDetailView();
        } catch (IllegalStateException ex) {
            throw new BizException(
                    SharedErrorDescriptors.CONFLICT,
                    "Only published form metadata can be deprecated",
                    ex
            );
        }
    }

    public FormMetadataDetailView deriveNewVersion(UUID metadataId) {
        FormMetadata source = loadRequired(metadataId);
        int nextVersion = repository.findByCode(source.tenantId(), source.code()).stream()
                .mapToInt(FormMetadata::version)
                .max()
                .orElse(source.version()) + 1;
        ensureVersionAvailable(source.tenantId(), source.code(), nextVersion);
        try {
            return repository.save(source.deriveNewVersion(UUID.randomUUID(), nextVersion, now())).toDetailView();
        } catch (IllegalStateException ex) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Draft metadata cannot derive new version", ex);
        }
    }

    public FormMetadataDetailView get(UUID metadataId) {
        return loadRequired(metadataId).toDetailView();
    }

    public List<FormMetadataView> query(FormMetadataCommands.FormMetadataQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return repository.findByTenant(query.tenantId()).stream()
                .filter(metadata -> query.code() == null || metadata.code().contains(query.code().trim()))
                .filter(metadata -> query.status() == null || metadata.status() == query.status())
                .sorted(UPDATED_DESC)
                .map(FormMetadata::toView)
                .toList();
    }

    public List<FormMetadataView> versions(UUID tenantId, String code) {
        List<FormMetadataView> versions = repository.findByCode(tenantId, code).stream()
                .sorted(VERSION_DESC)
                .map(FormMetadata::toView)
                .toList();
        if (versions.isEmpty()) {
            throw notFound();
        }
        return versions;
    }

    public FormRenderSchemaView latestRenderSchema(UUID tenantId, String code) {
        return repository.findLatestPublished(tenantId, code)
                .orElseThrow(this::notFound)
                .toRenderSchemaView();
    }

    public FormRenderSchemaView renderSchema(UUID tenantId, String code, int version) {
        FormMetadata metadata = repository.findByCodeAndVersion(tenantId, code, version)
                .filter(value -> value.status() == FormMetadataStatus.PUBLISHED)
                .orElseThrow(this::notFound);
        return metadata.toRenderSchemaView();
    }

    public FormMetadataValidationReport validate(UUID metadataId) {
        FormMetadata metadata = loadRequired(metadataId);
        return validationReport(
                metadata.fieldSchema(),
                metadata.layout(),
                metadata.validations(),
                metadata.fieldPermissionMap()
        );
    }

    private FormMetadata loadRequired(UUID metadataId) {
        return repository.findById(metadataId).orElseThrow(this::notFound);
    }

    private void validateMetadata(
            List<FormFieldDefinition> fields,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap
    ) {
        FormMetadataValidationReport report = validationReport(fields, layout, validations, fieldPermissionMap);
        if (!report.valid()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Invalid form metadata: " + report.issues().get(0).message()
            );
        }
    }

    private FormMetadataValidationReport validationReport(
            List<FormFieldDefinition> fields,
            JsonNode layout,
            JsonNode validations,
            JsonNode fieldPermissionMap
    ) {
        List<FormMetadataValidationIssue> issues = new ArrayList<>();
        Set<String> fieldCodes = new HashSet<>();
        try {
            FormMetadata probe = new FormMetadata(
                    UUID.randomUUID(),
                    "VALIDATION_ONLY",
                    "Validation Only",
                    null,
                    1,
                    FormMetadataStatus.DRAFT,
                    fields,
                    com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                    null,
                    null,
                    UUID.randomUUID(),
                    null,
                    now(),
                    now()
            );
            probe.fieldSchema().forEach(field -> field.flatten().forEach(flattened -> fieldCodes.add(flattened.fieldCode())));
        } catch (IllegalArgumentException ex) {
            issues.add(new FormMetadataValidationIssue("fieldSchema", "FORM_FIELD_SCHEMA_INVALID", ex.getMessage()));
        }
        int permissionNodeCount = validatePermissionMap(fieldPermissionMap, fieldCodes, issues);
        validateFieldReferences(layout, fieldCodes, "layout", issues);
        validateFieldReferences(validations, fieldCodes, "validations", issues);
        if (issues.isEmpty()) {
            return FormMetadataValidationReport.valid(fieldCodes.size(), permissionNodeCount);
        }
        return FormMetadataValidationReport.invalid(fieldCodes.size(), permissionNodeCount, issues);
    }

    private int validatePermissionMap(
            JsonNode fieldPermissionMap,
            Set<String> fieldCodes,
            List<FormMetadataValidationIssue> issues
    ) {
        if (fieldPermissionMap == null || fieldPermissionMap.isNull()) {
            return 0;
        }
        if (!fieldPermissionMap.isObject()) {
            issues.add(new FormMetadataValidationIssue(
                    "fieldPermissionMap",
                    "FORM_PERMISSION_MAP_INVALID",
                    "fieldPermissionMap must be an object"
            ));
            return 0;
        }
        int permissionNodeCount = 0;
        Iterator<Map.Entry<String, JsonNode>> nodeIterator = fieldPermissionMap.fields();
        while (nodeIterator.hasNext()) {
            Map.Entry<String, JsonNode> nodeEntry = nodeIterator.next();
            String nodeId = nodeEntry.getKey();
            JsonNode nodePermissions = nodeEntry.getValue();
            if (!nodePermissions.isObject()) {
                issues.add(new FormMetadataValidationIssue(
                        "fieldPermissionMap." + nodeId,
                        "FORM_PERMISSION_MAP_INVALID",
                        "node permission must be an object"
                ));
                continue;
            }
            permissionNodeCount++;
            Iterator<Map.Entry<String, JsonNode>> fieldIterator = nodePermissions.fields();
            while (fieldIterator.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = fieldIterator.next();
                String fieldCode = fieldEntry.getKey();
                if (!fieldCodes.contains(fieldCode)) {
                    issues.add(new FormMetadataValidationIssue(
                            "fieldPermissionMap." + nodeId + "." + fieldCode,
                            "FORM_PERMISSION_FIELD_NOT_FOUND",
                            "permission references unknown fieldCode: " + fieldCode
                    ));
                }
                validatePermissionFlags(fieldEntry.getValue(), "fieldPermissionMap." + nodeId + "." + fieldCode, issues);
            }
        }
        return permissionNodeCount;
    }

    private void validatePermissionFlags(
            JsonNode permission,
            String path,
            List<FormMetadataValidationIssue> issues
    ) {
        if (!permission.isObject()) {
            issues.add(new FormMetadataValidationIssue(path, "FORM_PERMISSION_MAP_INVALID",
                    "field permission must be an object"));
            return;
        }
        for (String flag : List.of("visible", "editable", "required")) {
            JsonNode value = permission.get(flag);
            if (value != null && !value.isBoolean()) {
                issues.add(new FormMetadataValidationIssue(path + "." + flag,
                        "FORM_PERMISSION_FLAG_INVALID", "permission flag must be boolean: " + flag));
            }
        }
    }

    private void validateFieldReferences(
            JsonNode node,
            Set<String> fieldCodes,
            String path,
            List<FormMetadataValidationIssue> issues
    ) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            JsonNode fieldCodeNode = node.get("fieldCode");
            if (fieldCodeNode != null && fieldCodeNode.isTextual() && !fieldCodes.contains(fieldCodeNode.asText())) {
                issues.add(new FormMetadataValidationIssue(
                        path + ".fieldCode",
                        "FORM_FIELD_REFERENCE_NOT_FOUND",
                        "metadata references unknown fieldCode: " + fieldCodeNode.asText()
                ));
            }
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                validateFieldReferences(entry.getValue(), fieldCodes, path + "." + entry.getKey(), issues);
            }
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                validateFieldReferences(node.get(index), fieldCodes, path + "[" + index + "]", issues);
            }
        }
    }

    private void ensureVersionAvailable(UUID tenantId, String code, int version) {
        repository.findByCodeAndVersion(tenantId, code, version)
                .ifPresent(metadata -> {
                    throw new BizException(SharedErrorDescriptors.CONFLICT, "Form metadata version already exists");
                });
    }

    private BizException notFound() {
        return new BizException(SharedErrorDescriptors.RESOURCE_NOT_FOUND, "Form metadata not found");
    }

    private Instant now() {
        return clock.instant();
    }
}
