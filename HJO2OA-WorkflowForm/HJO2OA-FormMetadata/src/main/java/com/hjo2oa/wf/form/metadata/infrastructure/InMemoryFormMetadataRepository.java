package com.hjo2oa.wf.form.metadata.infrastructure;

import com.hjo2oa.wf.form.metadata.domain.FormMetadata;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataRepository;
import com.hjo2oa.wf.form.metadata.domain.FormMetadataStatus;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnMissingBean(DataSource.class)
public class InMemoryFormMetadataRepository implements FormMetadataRepository {

    private static final Comparator<FormMetadata> VERSION_DESC = Comparator
            .comparing(FormMetadata::version)
            .reversed();

    private final Map<UUID, FormMetadata> metadataById = new LinkedHashMap<>();

    @Override
    public synchronized Optional<FormMetadata> findById(UUID metadataId) {
        return Optional.ofNullable(metadataById.get(metadataId));
    }

    @Override
    public synchronized Optional<FormMetadata> findByCodeAndVersion(UUID tenantId, String code, int version) {
        return metadataById.values().stream()
                .filter(metadata -> metadata.tenantId().equals(tenantId))
                .filter(metadata -> metadata.code().equals(normalizeCode(code)))
                .filter(metadata -> metadata.version() == version)
                .findFirst();
    }

    @Override
    public synchronized Optional<FormMetadata> findLatestPublished(UUID tenantId, String code) {
        return metadataById.values().stream()
                .filter(metadata -> metadata.tenantId().equals(tenantId))
                .filter(metadata -> metadata.code().equals(normalizeCode(code)))
                .filter(metadata -> metadata.status() == FormMetadataStatus.PUBLISHED)
                .max(Comparator.comparing(FormMetadata::version));
    }

    @Override
    public synchronized List<FormMetadata> findByCode(UUID tenantId, String code) {
        return metadataById.values().stream()
                .filter(metadata -> metadata.tenantId().equals(tenantId))
                .filter(metadata -> metadata.code().equals(normalizeCode(code)))
                .sorted(VERSION_DESC)
                .toList();
    }

    @Override
    public synchronized List<FormMetadata> findByTenant(UUID tenantId) {
        return metadataById.values().stream()
                .filter(metadata -> metadata.tenantId().equals(tenantId))
                .sorted(Comparator.comparing(FormMetadata::updatedAt).reversed())
                .toList();
    }

    @Override
    public synchronized FormMetadata save(FormMetadata metadata) {
        ensureUniqueVersion(metadata);
        metadataById.put(metadata.id(), Objects.requireNonNull(metadata, "metadata must not be null"));
        return metadata;
    }

    private void ensureUniqueVersion(FormMetadata metadata) {
        findByCodeAndVersion(metadata.tenantId(), metadata.code(), metadata.version())
                .filter(existing -> !existing.id().equals(metadata.id()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("form metadata unique key conflict");
                });
    }

    private String normalizeCode(String code) {
        return code == null ? null : code.trim();
    }
}
