package com.hjo2oa.data.report.interfaces;

import com.hjo2oa.data.report.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReportStatusRequest(
        @NotNull ReportStatus status
) {
}
