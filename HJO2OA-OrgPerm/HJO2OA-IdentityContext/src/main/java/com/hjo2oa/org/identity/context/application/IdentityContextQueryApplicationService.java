package com.hjo2oa.org.identity.context.application;

import com.hjo2oa.org.identity.context.domain.AvailableIdentityOption;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.domain.IdentityContextView;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class IdentityContextQueryApplicationService {

    private final IdentityContextSessionRepository sessionRepository;

    public IdentityContextQueryApplicationService(IdentityContextSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public IdentityContextView current() {
        try {
            return sessionRepository.currentSession().currentContext();
        } catch (IllegalStateException ex) {
            throw IdentityContextOperationException.conflict(
                    "IDENTITY_CONTEXT_UNAVAILABLE",
                    "Current identity context is unavailable"
            );
        }
    }

    public List<AvailableIdentityOption> available(boolean includePrimary) {
        try {
            return sessionRepository.currentSession().availableOptions(includePrimary);
        } catch (IllegalStateException ex) {
            throw IdentityContextOperationException.conflict(
                    "IDENTITY_CONTEXT_UNAVAILABLE",
                    "Current identity context is unavailable"
            );
        }
    }
}
