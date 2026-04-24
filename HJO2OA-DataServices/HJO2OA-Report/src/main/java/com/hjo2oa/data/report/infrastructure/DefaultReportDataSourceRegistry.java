package com.hjo2oa.data.report.infrastructure;

import com.hjo2oa.data.report.domain.ReportDataSourceProvider;
import com.hjo2oa.data.report.domain.ReportDataSourceRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultReportDataSourceRegistry implements ReportDataSourceRegistry {

    private final Map<String, ReportDataSourceProvider> providers;

    public DefaultReportDataSourceRegistry(List<ReportDataSourceProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(ReportDataSourceProvider::providerKey, Function.identity()));
    }

    @Override
    public Optional<ReportDataSourceProvider> find(String providerKey) {
        return Optional.ofNullable(providers.get(providerKey));
    }
}
