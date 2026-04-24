package com.hjo2oa.infra.tenant.interfaces;

import com.hjo2oa.infra.tenant.domain.TenantProfileView;
import com.hjo2oa.infra.tenant.domain.TenantQuotaView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TenantProfileDtoMapper {

    public TenantProfileDtos.TenantProfileResponse toResponse(TenantProfileView view) {
        return new TenantProfileDtos.TenantProfileResponse(
                view.id(),
                view.tenantCode(),
                view.name(),
                view.status(),
                view.isolationMode(),
                view.packageCode(),
                view.defaultLocale(),
                view.defaultTimezone(),
                view.initialized(),
                view.createdAt(),
                view.updatedAt()
        );
    }

    public TenantProfileDtos.TenantProfileDetailResponse toDetailResponse(
            TenantProfileView view,
            List<TenantQuotaView> quotas
    ) {
        return new TenantProfileDtos.TenantProfileDetailResponse(
                view.id(),
                view.tenantCode(),
                view.name(),
                view.status(),
                view.isolationMode(),
                view.packageCode(),
                view.defaultLocale(),
                view.defaultTimezone(),
                view.initialized(),
                view.createdAt(),
                view.updatedAt(),
                quotas.stream().map(this::toResponse).toList()
        );
    }

    public TenantProfileDtos.TenantQuotaResponse toResponse(TenantQuotaView view) {
        return new TenantProfileDtos.TenantQuotaResponse(
                view.id(),
                view.tenantProfileId(),
                view.quotaType(),
                view.limitValue(),
                view.usedValue(),
                view.warningThreshold(),
                view.warning()
        );
    }
}
