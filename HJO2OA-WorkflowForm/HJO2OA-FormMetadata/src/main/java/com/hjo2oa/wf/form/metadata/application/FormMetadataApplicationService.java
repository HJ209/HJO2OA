package com.hjo2oa.wf.form.metadata.application;

import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.wf.form.metadata.domain.FormFieldDefinition;
import com.hjo2oa.wf.form.metadata.domain.FormMetadata;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataDetailView;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataRepository;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataView;
import com.hjo2oa.wf.form.metadata.domain.FormRenderSchemaView;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
        validateFieldSchema(command.fieldSchema());
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
        validateFieldSchema(command.fieldSchema());
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

    public List<FormFieldDefinition> fields(UUID metadataId) {
        return loadRequired(metadataId).sortedFieldSchema();
    }

    private FormMetadata loadRequired(UUID metadataId) {
        return repository.findById(metadataId).orElseThrow(this::notFound);
    }

    private void validateFieldSchema(List<FormFieldDefinition> fields) {
        try {
            new FormMetadata(
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
        } catch (IllegalArgumentException ex) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Invalid form field schema: " + ex.getMessage(),
                    ex
            );
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
