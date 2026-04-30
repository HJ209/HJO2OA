package com.hjo2oa.wf.form.metadata.domain;

import java.util.List;

public record FormMetadataValidationReport(
        boolean valid,
        int fieldCount,
        int permissionNodeCount,
        List<FormMetadataValidationIssue> issues
) {

    public FormMetadataValidationReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public static FormMetadataValidationReport valid(int fieldCount, int permissionNodeCount) {
        return new FormMetadataValidationReport(true, fieldCount, permissionNodeCount, List.of());
    }

    public static FormMetadataValidationReport invalid(
            int fieldCount,
            int permissionNodeCount,
            List<FormMetadataValidationIssue> issues
    ) {
        return new FormMetadataValidationReport(false, fieldCount, permissionNodeCount, issues);
    }
}
