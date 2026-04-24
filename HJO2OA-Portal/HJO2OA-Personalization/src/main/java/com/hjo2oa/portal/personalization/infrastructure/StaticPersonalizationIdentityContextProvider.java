package com.hjo2oa.portal.personalization.infrastructure;

import com.hjo2oa.org.identity.context.application.IdentityContextQueryApplicationService;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import org.springframework.stereotype.Component;

@Component
public class StaticPersonalizationIdentityContextProvider implements PersonalizationIdentityContextProvider {

    private final IdentityContextQueryApplicationService queryApplicationService;

    public StaticPersonalizationIdentityContextProvider(
            IdentityContextQueryApplicationService queryApplicationService
    ) {
        this.queryApplicationService = queryApplicationService;
    }

    @Override
    public PersonalizationIdentityContext currentContext() {
        try {
            IdentityContextView identityContext = queryApplicationService.current();
            return new PersonalizationIdentityContext(
                    identityContext.tenantId(),
                    identityContext.personId(),
                    identityContext.currentAssignmentId(),
                    identityContext.currentPositionId()
            );
        } catch (RuntimeException ex) {
            throw new BizException(SharedErrorDescriptors.CONFLICT, "Current identity context is unavailable", ex);
        }
    }
}
