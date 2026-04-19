package com.hjo2oa.portal.aggregation.api.infrastructure;

import com.hjo2oa.org.identity.context.application.IdentityContextQueryApplicationService;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCardDataProvider;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.stereotype.Component;

@Component
public class IdentityContextPortalIdentityCardDataProvider implements PortalIdentityCardDataProvider {

    private final IdentityContextQueryApplicationService queryApplicationService;

    public IdentityContextPortalIdentityCardDataProvider(IdentityContextQueryApplicationService queryApplicationService) {
        this.queryApplicationService = queryApplicationService;
    }

    @Override
    public PortalIdentityCard currentIdentity() {
        try {
            IdentityContextView view = queryApplicationService.current();
            return new PortalIdentityCard(
                    view.tenantId(),
                    view.personId(),
                    view.accountId(),
                    view.currentAssignmentId(),
                    view.currentPositionId(),
                    view.currentOrganizationId(),
                    view.currentDepartmentId(),
                    view.currentPositionName(),
                    view.currentOrganizationName(),
                    view.currentDepartmentName(),
                    view.assignmentType().name(),
                    view.effectiveAt()
            );
        } catch (RuntimeException ex) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Current identity context is unavailable", ex);
        }
    }
}
