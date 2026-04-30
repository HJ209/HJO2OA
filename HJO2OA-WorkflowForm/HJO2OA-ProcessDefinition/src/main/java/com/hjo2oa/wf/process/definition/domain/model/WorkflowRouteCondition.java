package com.hjo2oa.wf.process.definition.domain.model;

public record WorkflowRouteCondition(
        String field,
        String operator,
        String expectedValue,
        String expression
) {
}
