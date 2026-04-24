package com.hjo2oa.data.data.sync.interfaces;

import com.hjo2oa.data.data.sync.application.SyncMappingRuleDraft;
import com.hjo2oa.data.data.sync.domain.ConflictStrategy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

public record SyncMappingRuleRequest(
        @NotBlank String sourceField,
        @NotBlank String targetField,
        Map<String, Object> transformRule,
        @NotNull ConflictStrategy conflictStrategy,
        boolean keyMapping,
        @PositiveOrZero int sortOrder
) {

    public SyncMappingRuleDraft toDraft() {
        return new SyncMappingRuleDraft(
                sourceField,
                targetField,
                transformRule,
                conflictStrategy,
                keyMapping,
                sortOrder
        );
    }
}
