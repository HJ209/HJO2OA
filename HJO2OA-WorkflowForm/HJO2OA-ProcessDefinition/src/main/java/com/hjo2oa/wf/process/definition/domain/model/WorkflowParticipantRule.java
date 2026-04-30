package com.hjo2oa.wf.process.definition.domain.model;

import java.util.List;
import java.util.Map;

public record WorkflowParticipantRule(
        String type,
        List<String> ids,
        String refId,
        String refFieldCode,
        String expression,
        Map<String, Object> attributes
) {
}
