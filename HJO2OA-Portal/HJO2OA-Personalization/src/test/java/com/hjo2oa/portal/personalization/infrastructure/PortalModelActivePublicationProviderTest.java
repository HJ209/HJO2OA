package com.hjo2oa.portal.personalization.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PortalModelActivePublicationProviderTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T08:00:00Z");

    @Test
    void shouldResolveAssignmentScopedPublicationForMatchingIdentity() {
        PortalPublicationApplicationService applicationService = publicationApplicationService();
        AtomicReference<PersonalizationIdentityContext> identity = new AtomicReference<>(identityContext("assignment-1"));
        PortalModelActivePublicationProvider provider = new PortalModelActivePublicationProvider(
                applicationService,
                identity::get
        );
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-office-default",
                "template-1",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-office-assignment",
                "template-2",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-1")
        ));

        assertThat(provider.findActivePublicationId(PersonalizationSceneType.OFFICE_CENTER))
                .contains("publication-office-assignment");
    }

    @Test
    void shouldFallBackToTenantDefaultPublicationWhenScopedPublicationDoesNotMatchIdentity() {
        PortalPublicationApplicationService applicationService = publicationApplicationService();
        AtomicReference<PersonalizationIdentityContext> identity = new AtomicReference<>(identityContext("assignment-2"));
        PortalModelActivePublicationProvider provider = new PortalModelActivePublicationProvider(
                applicationService,
                identity::get
        );
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-mobile-default",
                "template-3",
                PortalPublicationSceneType.MOBILE_WORKBENCH,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-mobile-assignment",
                "template-4",
                PortalPublicationSceneType.MOBILE_WORKBENCH,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.ofAssignment("assignment-1")
        ));

        assertThat(provider.findActivePublicationId(PersonalizationSceneType.MOBILE_WORKBENCH))
                .contains("publication-mobile-default");
    }

    @Test
    void shouldReturnEmptyWhenSceneHasNoActivePublication() {
        PortalModelActivePublicationProvider provider = new PortalModelActivePublicationProvider(
                publicationApplicationService(),
                () -> identityContext("assignment-1")
        );

        assertThat(provider.findActivePublicationId(PersonalizationSceneType.HOME)).isEmpty();
    }

    private PortalPublicationApplicationService publicationApplicationService() {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext(
                "tenant-1",
                "person-1"
        );
        return new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private PersonalizationIdentityContext identityContext(String assignmentId) {
        return new PersonalizationIdentityContext(
                "tenant-1",
                "person-1",
                assignmentId,
                "position-1"
        );
    }
}
