package com.hjo2oa.portal.personalization.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.org.identity.context.application.IdentityContextQueryApplicationService;
import com.hjo2oa.org.identity.context.domain.IdentityContextSession;
import com.hjo2oa.org.identity.context.domain.IdentityContextSessionRepository;
import com.hjo2oa.org.identity.context.infrastructure.InMemoryIdentityContextSessionRepository;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.shared.kernel.BizException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StaticPersonalizationIdentityContextProviderTest {

    @Test
    void shouldResolveCurrentIdentityFromIdentityContextSession() {
        InMemoryIdentityContextSessionRepository sessionRepository = new InMemoryIdentityContextSessionRepository();
        IdentityContextSession switchedSession = sessionRepository.currentSession()
                .withCurrentAssignment("assignment-2", 2L, Instant.parse("2026-04-20T01:00:00Z"));
        sessionRepository.save(switchedSession);
        StaticPersonalizationIdentityContextProvider provider = new StaticPersonalizationIdentityContextProvider(
                new IdentityContextQueryApplicationService(sessionRepository)
        );

        PersonalizationIdentityContext currentContext = provider.currentContext();

        assertThat(currentContext.tenantId()).isEqualTo("tenant-1");
        assertThat(currentContext.personId()).isEqualTo("person-1");
        assertThat(currentContext.assignmentId()).isEqualTo("assignment-2");
        assertThat(currentContext.positionId()).isEqualTo("position-2");
    }

    @Test
    void shouldTranslateUnavailableIdentityContextToConflict() {
        IdentityContextSessionRepository unavailableRepository = new IdentityContextSessionRepository() {
            @Override
            public IdentityContextSession currentSession() {
                throw new IllegalStateException("session unavailable");
            }

            @Override
            public IdentityContextSession save(IdentityContextSession session) {
                return session;
            }
        };
        StaticPersonalizationIdentityContextProvider provider = new StaticPersonalizationIdentityContextProvider(
                new IdentityContextQueryApplicationService(unavailableRepository)
        );

        assertThatThrownBy(provider::currentContext)
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Current identity context is unavailable");
    }
}
