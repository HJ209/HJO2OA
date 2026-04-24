package com.hjo2oa.data.report.domain;

import java.util.List;

public record ReportDefinitionPage(
        List<ReportDefinition> items,
        long total
) {

    public ReportDefinitionPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
