package com.hjo2oa.data.report.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public record ReportDataRecord(
        Instant occurredAt,
        Map<String, Object> fields
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ReportDataRecord {
        fields = fields == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(fields));
    }

    public Object field(String fieldCode) {
        return fields.get(fieldCode);
    }
}
