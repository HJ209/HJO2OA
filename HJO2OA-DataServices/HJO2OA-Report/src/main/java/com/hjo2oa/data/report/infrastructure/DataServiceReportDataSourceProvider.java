package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.domain.ReportDataFetchRequest;
import com.hjo2oa.data.report.domain.ReportDataRecord;
import com.hjo2oa.data.report.domain.ReportDataSourceProvider;
import com.hjo2oa.data.service.application.DataServiceInvocationApplicationService;
import com.hjo2oa.data.service.application.DataServiceInvocationCommands;
import com.hjo2oa.data.service.domain.DataServiceViews;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DataServiceReportDataSourceProvider implements ReportDataSourceProvider {

    public static final String PROVIDER_KEY = "data-service";
    private static final int DEFAULT_PREVIEW_ROWS = 12;

    private final DataServiceInvocationApplicationService dataServiceInvocationApplicationService;

    public DataServiceReportDataSourceProvider(
            DataServiceInvocationApplicationService dataServiceInvocationApplicationService
    ) {
        this.dataServiceInvocationApplicationService = Objects.requireNonNull(
                dataServiceInvocationApplicationService,
                "dataServiceInvocationApplicationService must not be null");
    }

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public List<ReportDataRecord> fetch(ReportDataFetchRequest request) {
        String serviceCode = requireText(request.dataServiceCode(), "dataServiceCode");
        DataServiceViews.ExecutionPlan executionPlan = dataServiceInvocationApplicationService.query(
                new DataServiceInvocationCommands.InvocationCommand(
                        serviceCode,
                        "report",
                        request.subjectCode(),
                        "report:" + request.reportCode() + ":" + request.requestedAt(),
                        new LinkedHashMap<>(request.baseFilters())
                )
        );
        if (!executionPlan.reportReusable()) {
            throw new IllegalStateException("data service is not reusable by report: " + serviceCode);
        }
        return materializeRows(request, executionPlan);
    }

    private List<ReportDataRecord> materializeRows(
            ReportDataFetchRequest request,
            DataServiceViews.ExecutionPlan executionPlan
    ) {
        int rowCount = Math.max(1, Math.min(request.maxRows(), DEFAULT_PREVIEW_ROWS));
        Instant requestedAt = request.requestedAt();
        return java.util.stream.IntStream.range(0, rowCount)
                .mapToObj(index -> {
                    Instant occurredAt = requestedAt.minusSeconds((long) (rowCount - index - 1) * 86_400L);
                    Map<String, Object> fields = new LinkedHashMap<>();
                    fields.put("id", executionPlan.serviceCode() + "-" + (index + 1));
                    fields.put("time", occurredAt.toString());
                    fields.put("total", index + 1);
                    fields.put("value", index + 1);
                    fields.put("count", index + 1);
                    if (request.defaultTimeField() != null) {
                        fields.put(request.defaultTimeField(), occurredAt.toString());
                    }
                    request.baseFilters().forEach(fields::putIfAbsent);
                    for (String outputField : executionPlan.outputFields()) {
                        fields.putIfAbsent(outputField, fieldValue(outputField, index, occurredAt, executionPlan));
                    }
                    return new ReportDataRecord(occurredAt, fields);
                })
                .toList();
    }

    private Object fieldValue(
            String field,
            int index,
            Instant occurredAt,
            DataServiceViews.ExecutionPlan executionPlan
    ) {
        Object parameterValue = executionPlan.normalizedParameters().get(field);
        if (parameterValue != null) {
            return parameterValue;
        }
        if ("id".equalsIgnoreCase(field)) {
            return executionPlan.serviceCode() + "-" + (index + 1);
        }
        if ("time".equalsIgnoreCase(field)
                || "date".equalsIgnoreCase(field)
                || "occurredAt".equalsIgnoreCase(field)
                || "createdAt".equalsIgnoreCase(field)) {
            return occurredAt.toString();
        }
        return index + 1;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required for data-service report source");
        }
        return value.trim();
    }
}
