package com.hjo2oa.data.report.interfaces;

public record RefreshReportRequest(
        String reason,
        String batchId
) {
}
