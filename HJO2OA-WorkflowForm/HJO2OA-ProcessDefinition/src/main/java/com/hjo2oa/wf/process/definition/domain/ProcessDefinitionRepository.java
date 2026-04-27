package com.hjo2oa.wf.process.definition.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessDefinitionRepository {

    Optional<ProcessDefinition> findById(UUID definitionId);

    List<ProcessDefinition> findByCode(UUID tenantId, String code);

    Optional<ProcessDefinition> findByCodeAndVersion(UUID tenantId, String code, int version);

    List<ProcessDefinition> findByTenantCategoryAndStatus(UUID tenantId, String category, DefinitionStatus status);

    ProcessDefinition save(ProcessDefinition definition);

    void delete(UUID definitionId);
}
