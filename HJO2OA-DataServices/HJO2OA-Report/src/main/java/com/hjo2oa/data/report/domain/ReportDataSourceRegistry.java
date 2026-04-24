package com.hjo2oa.data.report.domain;

import java.util.Optional;

public interface ReportDataSourceRegistry {

    Optional<ReportDataSourceProvider> find(String providerKey);
}
