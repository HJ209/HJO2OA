package com.hjo2oa.data.report.application;

import com.hjo2oa.data.report.domain.ReportCaliberDefinition;
import com.hjo2oa.data.report.domain.ReportCardProtocol;
import com.hjo2oa.data.report.domain.ReportDimensionDefinition;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import com.hjo2oa.data.report.domain.ReportRefreshConfig;
import com.hjo2oa.data.report.domain.ReportRefreshMode;
import com.hjo2oa.data.report.domain.ReportSourceScope;
import com.hjo2oa.data.report.domain.ReportType;
import com.hjo2oa.data.report.domain.ReportVisibilityMode;
import java.util.List;

public record SaveReportDefinitionCommand(
        String code,
        String name,
        ReportType reportType,
        ReportSourceScope sourceScope,
        ReportRefreshMode refreshMode,
        ReportVisibilityMode visibilityMode,
        String tenantId,
        ReportCaliberDefinition caliberDefinition,
        ReportRefreshConfig refreshConfig,
        ReportCardProtocol cardProtocol,
        List<ReportMetricDefinition> metrics,
        List<ReportDimensionDefinition> dimensions
) {
}
