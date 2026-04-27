package com.hjo2oa.wf.process.instance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessInstanceRepository {

    Optional<ProcessInstance> findById(UUID instanceId);

    List<ProcessInstance> findByInitiator(UUID tenantId, UUID initiatorId);

    ProcessInstance save(ProcessInstance instance);
}
