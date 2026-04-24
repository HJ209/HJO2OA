package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegionView;
import com.hjo2oa.portal.portal.model.domain.PortalPageView;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacementView;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceState;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatus;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceStatusRepository;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetReferenceViolation;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetDisabledEvent;
import com.hjo2oa.portal.widget.config.domain.PortalWidgetUpdatedEvent;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PortalWidgetReferenceStatusApplicationService {

    private final PortalWidgetReferenceStatusRepository statusRepository;

    public PortalWidgetReferenceStatusApplicationService(
            PortalWidgetReferenceStatusRepository statusRepository
    ) {
        this.statusRepository = Objects.requireNonNull(statusRepository, "statusRepository must not be null");
    }

    public Optional<PortalWidgetReferenceStatus> current(String widgetId) {
        Objects.requireNonNull(widgetId, "widgetId must not be null");
        return statusRepository.findByWidgetId(widgetId);
    }

    public Optional<PortalWidgetReferenceStatus> current(String tenantId, String widgetCode) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(widgetCode, "widgetCode must not be null");
        return statusRepository.findByWidgetCode(tenantId, widgetCode);
    }

    public void ensureNoRepairRequiredReferences(String tenantId, List<PortalPageView> pages) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        List<PortalPageView> safePages = List.copyOf(Objects.requireNonNull(pages, "pages must not be null"));
        List<PortalWidgetReferenceViolation> violations = safePages.stream()
                .flatMap(page -> page.regions().stream()
                        .flatMap(region -> region.placements().stream()
                                .map(placement -> violationOf(tenantId, page, region, placement))
                                .flatMap(Optional::stream)))
                .toList();
        if (!violations.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "Portal template contains repair-required widget references: "
                            + violations.stream()
                            .map(PortalWidgetReferenceViolation::describe)
                            .collect(Collectors.joining("; "))
            );
        }
    }

    public PortalWidgetReferenceStatus markUpdated(PortalWidgetUpdatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        PortalWidgetReferenceStatus status = PortalWidgetReferenceStatus.stale(event);
        statusRepository.save(status);
        return status;
    }

    public PortalWidgetReferenceStatus markDisabled(PortalWidgetDisabledEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        PortalWidgetReferenceStatus status = PortalWidgetReferenceStatus.repairRequired(event);
        statusRepository.save(status);
        return status;
    }

    public static PortalWidgetReferenceStatusApplicationService noop() {
        return new PortalWidgetReferenceStatusApplicationService(new PortalWidgetReferenceStatusRepository() {
            @Override
            public Optional<PortalWidgetReferenceStatus> findByWidgetId(String widgetId) {
                return Optional.empty();
            }

            @Override
            public Optional<PortalWidgetReferenceStatus> findByWidgetCode(String tenantId, String widgetCode) {
                return Optional.empty();
            }

            @Override
            public PortalWidgetReferenceStatus save(PortalWidgetReferenceStatus status) {
                return status;
            }
        });
    }

    private Optional<PortalWidgetReferenceViolation> violationOf(
            String tenantId,
            PortalPageView page,
            PortalLayoutRegionView region,
            PortalWidgetPlacementView placement
    ) {
        return statusRepository.findByWidgetCode(tenantId, placement.widgetCode())
                .filter(status -> status.state() == PortalWidgetReferenceState.REPAIR_REQUIRED)
                .map(status -> new PortalWidgetReferenceViolation(
                        status.widgetCode(),
                        placement.placementCode(),
                        page.pageCode(),
                        region.regionCode()
                ));
    }
}
