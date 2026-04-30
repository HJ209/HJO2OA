package com.hjo2oa.wf.process.instance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceRepository {

    Optional<ProcessInstance> findById(UUID instanceId);

    Optional<ProcessInstance> findByTenantAndIdempotencyKey(UUID tenantId, String idempotencyKey);

    List<ProcessInstance> findByInitiator(UUID tenantId, UUID initiatorId);

    ProcessInstance save(ProcessInstance instance);
}
