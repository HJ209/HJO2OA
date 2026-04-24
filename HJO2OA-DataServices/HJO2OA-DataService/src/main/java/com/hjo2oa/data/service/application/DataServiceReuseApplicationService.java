package com.hjo2oa.data.service.application;

import com.hjo2oa.data.service.domain.DataServiceDefinitionRepository;
import com.hjo2oa.data.service.domain.DataServiceViews;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DataServiceReuseApplicationService implements DataServiceDefinitionReuseGateway {

    private static final Comparator<DataServiceViews.ReusableView> REUSE_ORDER =
            Comparator.comparing(DataServiceViews.ReusableView::code)
                    .thenComparing(DataServiceViews.ReusableView::serviceId);

    private final DataServiceDefinitionRepository repository;

    public DataServiceReuseApplicationService(DataServiceDefinitionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Optional<DataServiceViews.ReusableView> findActivated(UUID tenantId, String serviceCode) {
        return repository.findActiveByCode(tenantId, serviceCode)
                .map(com.hjo2oa.data.service.domain.DataServiceDefinition::toReusableView);
    }

    @Override
    public List<DataServiceViews.ReusableView> listActivatedForOpenApi(UUID tenantId) {
        return repository.findAllActiveByTenant(tenantId).stream()
                .map(com.hjo2oa.data.service.domain.DataServiceDefinition::toReusableView)
                .filter(DataServiceViews.ReusableView::openApiReusable)
                .sorted(REUSE_ORDER)
                .toList();
    }

    @Override
    public List<DataServiceViews.ReusableView> listActivatedForReport(UUID tenantId) {
        return repository.findAllActiveByTenant(tenantId).stream()
                .map(com.hjo2oa.data.service.domain.DataServiceDefinition::toReusableView)
                .filter(DataServiceViews.ReusableView::reportReusable)
                .sorted(REUSE_ORDER)
                .toList();
    }
}
