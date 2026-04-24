package com.hjo2oa.data.report.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReportDataRecord(
        Instant occurredAt,
        Map<String, Object> fields
) {

    public ReportDataRecord {
        fields = fields == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(fields));
    }

    public Object field(String fieldCode) {
        return fields.get(fieldCode);
    }
}
