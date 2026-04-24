package com.hjo2oa.data.service.application;

import com.hjo2oa.data.service.domain.DataServiceViews;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DataServiceDefinitionReuseGateway {

    Optional<DataServiceViews.ReusableView> findActivated(UUID tenantId, String serviceCode);

    List<DataServiceViews.ReusableView> listActivatedForOpenApi(UUID tenantId);

    List<DataServiceViews.ReusableView> listActivatedForReport(UUID tenantId);
}
