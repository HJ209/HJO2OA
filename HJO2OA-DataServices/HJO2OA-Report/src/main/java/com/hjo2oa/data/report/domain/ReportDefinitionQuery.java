package com.hjo2oa.data.report.domain;

public record ReportDefinitionQuery(
        int page,
        int size,
        ReportType reportType,
        ReportSourceScope sourceScope,
        ReportStatus status,
        ReportVisibilityMode visibilityMode
) {
}
