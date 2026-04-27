package com.hjo2oa.wf.process.definition.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActionDefinitionRepository {

    Optional<ActionDefinition> findById(UUID actionId);

    Optional<ActionDefinition> findByTenantAndCode(UUID tenantId, String code);

    List<ActionDefinition> findByTenant(UUID tenantId);

    List<ActionDefinition> findByTenantAndCategory(UUID tenantId, ActionCategory category);

    ActionDefinition save(ActionDefinition actionDefinition);

    void delete(UUID actionId);
}
