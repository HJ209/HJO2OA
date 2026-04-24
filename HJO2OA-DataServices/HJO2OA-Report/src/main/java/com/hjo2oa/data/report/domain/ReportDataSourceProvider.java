package com.hjo2oa.data.report.domain;

import java.util.List;

public interface ReportDataSourceProvider {

    String providerKey();

    List<ReportDataRecord> fetch(ReportDataFetchRequest request);
}
