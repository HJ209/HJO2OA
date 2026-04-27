package com.hjo2oa.wf.form.metadata.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FormMetadataRepository {

    Optional<FormMetadata> findById(UUID metadataId);

    Optional<FormMetadata> findByCodeAndVersion(UUID tenantId, String code, int version);

    Optional<FormMetadata> findLatestPublished(UUID tenantId, String code);

    List<FormMetadata> findByCode(UUID tenantId, String code);

    List<FormMetadata> findByTenant(UUID tenantId);

    FormMetadata save(FormMetadata metadata);
}
