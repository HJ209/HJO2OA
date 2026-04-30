package com.hjo2oa.data.report.application;

public record ReportExportFile(
        String filename,
        String contentType,
        byte[] content
) {
}
