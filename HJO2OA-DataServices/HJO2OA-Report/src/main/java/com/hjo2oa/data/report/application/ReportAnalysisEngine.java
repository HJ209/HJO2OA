package com.hjo2oa.data.report.application;

import com.hjo2oa.data.report.domain.ReportAnalysisQuery;
import com.hjo2oa.data.report.domain.ReportDataRecord;
import com.hjo2oa.data.report.domain.ReportDefinition;
import com.hjo2oa.data.report.domain.ReportDimensionDefinition;
import com.hjo2oa.data.report.domain.ReportMetricAggregationType;
import com.hjo2oa.data.report.domain.ReportMetricDefinition;
import com.hjo2oa.data.report.domain.ReportMetricValue;
import com.hjo2oa.data.report.domain.ReportRankingItem;
import com.hjo2oa.data.report.domain.ReportTimeGranularity;
import com.hjo2oa.data.report.domain.ReportTrendPoint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ReportAnalysisEngine {

    public List<ReportMetricValue> summarize(
            ReportDefinition reportDefinition,
            List<ReportDataRecord> rows,
            ReportAnalysisQuery query
    ) {
        List<ReportDataRecord> filteredRows = filterRows(reportDefinition, rows, query);
        return reportDefinition.metrics().stream()
                .map(metric -> toMetricValue(metric, aggregateMetric(metric, filteredRows)))
                .toList();
    }

    public List<ReportTrendPoint> trend(
            ReportDefinition reportDefinition,
            List<ReportDataRecord> rows,
            ReportAnalysisQuery query
    ) {
        ReportMetricDefinition metricDefinition = resolveTrendMetric(reportDefinition, query.metricCode());
        ReportDimensionDefinition dimensionDefinition = resolveTrendDimension(reportDefinition, query.dimensionCode());
        List<ReportDataRecord> filteredRows = filterRows(reportDefinition, rows, query);
        Map<String, List<ReportDataRecord>> groupedRows = new TreeMap<>();
        for (ReportDataRecord row : filteredRows) {
            String bucket = bucketValue(row, dimensionDefinition);
            if (bucket == null) {
                continue;
            }
            groupedRows.computeIfAbsent(bucket, ignored -> new ArrayList<>()).add(row);
        }
        return groupedRows.entrySet().stream()
                .map(entry -> new ReportTrendPoint(entry.getKey(), aggregateMetric(metricDefinition, entry.getValue())))
                .toList();
    }

    public List<ReportRankingItem> ranking(
            ReportDefinition reportDefinition,
            List<ReportDataRecord> rows,
            ReportAnalysisQuery query
    ) {
        ReportMetricDefinition metricDefinition = resolveRankingMetric(reportDefinition, query.metricCode());
        ReportDimensionDefinition dimensionDefinition = resolveRankingDimension(reportDefinition, query.dimensionCode());
        List<ReportDataRecord> filteredRows = filterRows(reportDefinition, rows, query);
        Map<String, List<ReportDataRecord>> groupedRows = new LinkedHashMap<>();
        for (ReportDataRecord row : filteredRows) {
            String dimensionValue = dimensionValue(row, dimensionDefinition);
            if (dimensionValue == null) {
                continue;
            }
            groupedRows.computeIfAbsent(dimensionValue, ignored -> new ArrayList<>()).add(row);
        }
        List<Map.Entry<String, List<ReportDataRecord>>> sortedEntries = groupedRows.entrySet().stream()
                .sorted(Comparator.comparing(
                        entry -> aggregateMetric(metricDefinition, entry.getValue()),
                        Comparator.reverseOrder()
                ))
                .limit(query.resolvedTopN())
                .toList();
        List<ReportRankingItem> items = new ArrayList<>();
        for (int index = 0; index < sortedEntries.size(); index++) {
            Map.Entry<String, List<ReportDataRecord>> entry = sortedEntries.get(index);
            items.add(new ReportRankingItem(
                    index + 1,
                    entry.getKey(),
                    aggregateMetric(metricDefinition, entry.getValue())
            ));
        }
        return items;
    }

    public List<ReportDataRecord> filterForExport(
            ReportDefinition reportDefinition,
            List<ReportDataRecord> rows,
            ReportAnalysisQuery query
    ) {
        return filterRows(reportDefinition, rows, query);
    }

    private List<ReportDataRecord> filterRows(
            ReportDefinition reportDefinition,
            List<ReportDataRecord> rows,
            ReportAnalysisQuery query
    ) {
        ReportDimensionDefinition defaultTimeDimension = reportDefinition.defaultTimeDimension();
        Map<String, String> filters = query.filters();
        return rows.stream()
                .filter(row -> matchesTimeRange(row, reportDefinition, defaultTimeDimension, query.from(), query.to()))
                .filter(row -> matchesFilters(row, filters))
                .toList();
    }

    private boolean matchesTimeRange(
            ReportDataRecord row,
            ReportDefinition reportDefinition,
            ReportDimensionDefinition timeDimension,
            Instant from,
            Instant to
    ) {
        if (from == null && to == null) {
            return true;
        }
        Instant targetTime = resolveTime(row, reportDefinition, timeDimension);
        if (targetTime == null) {
            return false;
        }
        if (from != null && targetTime.isBefore(from)) {
            return false;
        }
        return to == null || !targetTime.isAfter(to);
    }

    private boolean matchesFilters(ReportDataRecord row, Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            Object actualValue = row.field(entry.getKey());
            if (!Objects.equals(stringify(actualValue), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private ReportMetricDefinition resolveTrendMetric(ReportDefinition reportDefinition, String metricCode) {
        if (metricCode != null && !metricCode.isBlank()) {
            ReportMetricDefinition metric = reportDefinition.metric(metricCode);
            if (metric != null) {
                return metric;
            }
        }
        return reportDefinition.metrics().stream()
                .filter(ReportMetricDefinition::trendEnabled)
                .findFirst()
                .orElse(reportDefinition.metrics().get(0));
    }

    private ReportMetricDefinition resolveRankingMetric(ReportDefinition reportDefinition, String metricCode) {
        if (metricCode != null && !metricCode.isBlank()) {
            ReportMetricDefinition metric = reportDefinition.metric(metricCode);
            if (metric != null) {
                return metric;
            }
        }
        return reportDefinition.metrics().stream()
                .filter(ReportMetricDefinition::rankEnabled)
                .findFirst()
                .orElse(reportDefinition.metrics().get(0));
    }

    private ReportDimensionDefinition resolveTrendDimension(ReportDefinition reportDefinition, String dimensionCode) {
        if (dimensionCode != null && !dimensionCode.isBlank()) {
            ReportDimensionDefinition dimension = reportDefinition.dimension(dimensionCode);
            if (dimension != null) {
                return dimension;
            }
        }
        ReportDimensionDefinition defaultTimeDimension = reportDefinition.defaultTimeDimension();
        if (defaultTimeDimension == null) {
            throw new IllegalArgumentException("report definition does not define a time dimension");
        }
        return defaultTimeDimension;
    }

    private ReportDimensionDefinition resolveRankingDimension(ReportDefinition reportDefinition, String dimensionCode) {
        if (dimensionCode != null && !dimensionCode.isBlank()) {
            ReportDimensionDefinition dimension = reportDefinition.dimension(dimensionCode);
            if (dimension != null) {
                return dimension;
            }
        }
        return reportDefinition.dimensions().stream()
                .filter(dimension -> !dimension.timeDimension())
                .findFirst()
                .or(() -> reportDefinition.dimensions().stream().findFirst())
                .orElseThrow(() -> new IllegalArgumentException("report definition does not define a ranking dimension"));
    }

    private ReportMetricValue toMetricValue(ReportMetricDefinition metricDefinition, BigDecimal value) {
        return new ReportMetricValue(
                metricDefinition.metricCode(),
                metricDefinition.metricName(),
                value,
                metricDefinition.unit()
        );
    }

    private BigDecimal aggregateMetric(ReportMetricDefinition metricDefinition, List<ReportDataRecord> rows) {
        return switch (metricDefinition.aggregationType()) {
            case COUNT -> BigDecimal.valueOf(rows.size());
            case SUM -> rows.stream()
                    .map(row -> decimalValue(row.field(metricDefinition.sourceField())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case AVG -> average(rows, metricDefinition.sourceField());
            case DISTINCT -> BigDecimal.valueOf(rows.stream()
                    .map(row -> stringify(row.field(metricDefinition.sourceField())))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet())
                    .size());
            case RATIO -> ratio(rows, metricDefinition.formula());
        };
    }

    private BigDecimal average(List<ReportDataRecord> rows, String sourceField) {
        List<BigDecimal> values = rows.stream()
                .map(row -> decimalValue(row.field(sourceField)))
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(List<ReportDataRecord> rows, String formula) {
        String normalizedFormula = formula == null ? "" : formula.replace(" ", "");
        String[] parts = normalizedFormula.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("unsupported ratio formula: " + formula);
        }
        BigDecimal numerator = aggregateFormulaPart(rows, parts[0]);
        BigDecimal denominator = aggregateFormulaPart(rows, parts[1]);
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal aggregateFormulaPart(List<ReportDataRecord> rows, String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        if ("count(*)".equals(normalizedToken)) {
            return BigDecimal.valueOf(rows.size());
        }
        if (normalizedToken.startsWith("count(") && normalizedToken.endsWith(")")) {
            String field = token.substring(6, token.length() - 1);
            long count = rows.stream().map(row -> row.field(field)).filter(Objects::nonNull).count();
            return BigDecimal.valueOf(count);
        }
        if (normalizedToken.startsWith("sum(") && normalizedToken.endsWith(")")) {
            String field = token.substring(4, token.length() - 1);
            return rows.stream()
                    .map(row -> decimalValue(row.field(field)))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        throw new IllegalArgumentException("unsupported ratio token: " + token);
    }

    private Instant resolveTime(
            ReportDataRecord row,
            ReportDefinition reportDefinition,
            ReportDimensionDefinition timeDimension
    ) {
        if (timeDimension != null) {
            Instant dimensionInstant = instantValue(row.field(timeDimension.sourceField()));
            if (dimensionInstant != null) {
                return dimensionInstant;
            }
        }
        if (reportDefinition.caliberDefinition().defaultTimeField() != null) {
            Instant caliberInstant = instantValue(row.field(reportDefinition.caliberDefinition().defaultTimeField()));
            if (caliberInstant != null) {
                return caliberInstant;
            }
        }
        return row.occurredAt();
    }

    private String bucketValue(ReportDataRecord row, ReportDimensionDefinition dimensionDefinition) {
        if (!dimensionDefinition.timeDimension()) {
            return dimensionValue(row, dimensionDefinition);
        }
        Instant instant = instantValue(row.field(dimensionDefinition.sourceField()));
        if (instant == null) {
            instant = row.occurredAt();
        }
        if (instant == null) {
            return null;
        }
        LocalDate localDate = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return switch (dimensionDefinition.timeGranularity()) {
            case DAY -> localDate.toString();
            case WEEK -> {
                WeekFields weekFields = WeekFields.ISO;
                int week = localDate.get(weekFields.weekOfWeekBasedYear());
                yield localDate.getYear() + "-W" + String.format(Locale.ROOT, "%02d", week);
            }
            case MONTH -> localDate.getYear() + "-" + String.format(Locale.ROOT, "%02d", localDate.getMonthValue());
            case NONE -> localDate.toString();
        };
    }

    private String dimensionValue(ReportDataRecord row, ReportDimensionDefinition dimensionDefinition) {
        return stringify(row.field(dimensionDefinition.sourceField()));
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return new BigDecimal(stringValue);
        }
        return BigDecimal.ZERO;
    }

    private Instant instantValue(Object value) {
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Instant.parse(text);
        }
        return null;
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }
}
