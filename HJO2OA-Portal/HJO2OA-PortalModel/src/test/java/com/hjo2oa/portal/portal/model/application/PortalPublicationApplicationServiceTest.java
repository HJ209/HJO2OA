package com.hjo2oa.portal.portal.model.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudienceType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PortalPublicationApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T06:00:00Z");

    @Test
    void shouldActivatePublicationAndPublishEvent() {
        InMemoryPortalPublicationRepository repository = new InMemoryPortalPublicationRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalPublicationApplicationService applicationService = applicationService(repository, publishedEvents);

        PortalPublicationView publication = applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        assertThat(publication.publicationId()).isEqualTo("publication-1");
        assertThat(publication.status().name()).isEqualTo("ACTIVE");
        assertThat(publication.clientType()).isEqualTo(PortalPublicationClientType.PC);
        assertThat(publication.audience().type()).isEqualTo(PortalPublicationAudienceType.TENANT_DEFAULT);
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalPublicationActivatedEvent.class);
    }

    @Test
    void shouldOfflinePublicationAndPublishEvent() {
        InMemoryPortalPublicationRepository repository = new InMemoryPortalPublicationRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PortalPublicationApplicationService applicationService = applicationService(repository, publishedEvents);
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        publishedEvents.clear();

        PortalPublicationView publication = applicationService.offline(new OfflinePortalPublicationCommand("publication-1"));

        assertThat(publication.status().name()).isEqualTo("OFFLINE");
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalPublicationOfflinedEvent.class);
    }

    @Test
    void shouldReturnCurrentActivePublicationBySceneAndClient() {
        PortalPublicationApplicationService applicationService = applicationService(
                new InMemoryPortalPublicationRepository(),
                new ArrayList<>()
        );
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        assertThat(applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC
        ))
                .hasValueSatisfying(publication ->
                        assertThat(publication.publicationId()).isEqualTo("publication-1")
                );
    }

    @Test
    void shouldNotReturnOfflinedPublicationFromActiveQuery() {
        PortalPublicationApplicationService applicationService = applicationService(
                new InMemoryPortalPublicationRepository(),
                new ArrayList<>()
        );
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        applicationService.offline(new OfflinePortalPublicationCommand("publication-1"));

        assertThat(applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL
        )).isEmpty();
    }

    @Test
    void shouldRejectDuplicateActivePublicationForSameSceneAndClient() {
        PortalPublicationApplicationService applicationService = applicationService(
                new InMemoryPortalPublicationRepository(),
                new ArrayList<>()
        );
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        assertThatThrownBy(() -> applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-2",
                "template-2",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Active publication already exists");
    }

    @Test
    void shouldResolveActivePublicationDeterministicallyAcrossAudienceSpecificity() {
        PortalPublicationApplicationService applicationService = applicationService(
                new InMemoryPortalPublicationRepository(),
                new ArrayList<>()
        );
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-position",
                "template-position",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPosition("position-1")
        ));
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-person",
                "template-person",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPerson("person-1")
        ));
        applicationService.activate(new ActivatePortalPublicationCommand(
                "publication-assignment",
                "template-assignment",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-1")
        ));

        assertThat(applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                new PortalPublicationIdentity("assignment-1", "position-1", "person-1")
        )).hasValueSatisfying(publication ->
                assertThat(publication.publicationId()).isEqualTo("publication-assignment"));

        assertThat(applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                new PortalPublicationIdentity(null, "position-1", "person-1")
        )).hasValueSatisfying(publication ->
                assertThat(publication.publicationId()).isEqualTo("publication-person"));

        assertThat(applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                new PortalPublicationIdentity(null, "position-1", null)
        )).hasValueSatisfying(publication ->
                assertThat(publication.publicationId()).isEqualTo("publication-position"));

        assertThat(applicationService.currentActive(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationIdentity.tenantDefault()
        )).hasValueSatisfying(publication ->
                assertThat(publication.publicationId()).isEqualTo("publication-default"));
    }

    @Test
    void shouldListPublicationsForCurrentTenantWithOptionalFilters() {
        InMemoryPortalPublicationRepository repository = new InMemoryPortalPublicationRepository();
        PortalPublicationApplicationService tenantOneService = applicationService(
                repository,
                "tenant-1",
                new ArrayList<>()
        );
        PortalPublicationApplicationService tenantTwoService = applicationService(
                repository,
                "tenant-2",
                new ArrayList<>()
        );
        tenantOneService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPerson("person-1")
        ));
        tenantOneService.activate(new ActivatePortalPublicationCommand(
                "publication-2",
                "template-2",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.ALL,
                PortalPublicationAudience.tenantDefault()
        ));
        tenantOneService.activate(new ActivatePortalPublicationCommand(
                "publication-3",
                "template-3",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofPosition("position-1")
        ));
        tenantOneService.offline(new OfflinePortalPublicationCommand("publication-3"));
        tenantTwoService.activate(new ActivatePortalPublicationCommand(
                "publication-4",
                "template-4",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        List<PortalPublicationView> allPublications = tenantOneService.list(null, null, null);
        List<PortalPublicationView> filteredPublications = tenantOneService.list(
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationStatus.ACTIVE
        );

        assertThat(allPublications).extracting(PortalPublicationView::publicationId)
                .containsExactly("publication-1", "publication-2", "publication-3");
        assertThat(allPublications).extracting(publication -> publication.audience().type())
                .containsExactly(
                        PortalPublicationAudienceType.PERSON,
                        PortalPublicationAudienceType.TENANT_DEFAULT,
                        PortalPublicationAudienceType.POSITION
                );
        assertThat(filteredPublications).extracting(PortalPublicationView::publicationId)
                .containsExactly("publication-1");
    }

    private PortalPublicationApplicationService applicationService(
            InMemoryPortalPublicationRepository repository,
            List<DomainEvent> publishedEvents
    ) {
        return applicationService(repository, "tenant-1", publishedEvents);
    }

    private PortalPublicationApplicationService applicationService(
            InMemoryPortalPublicationRepository repository,
            String tenantId,
            List<DomainEvent> publishedEvents
    ) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext(tenantId, "portal-admin");
        return new PortalPublicationApplicationService(
                repository,
                contextProvider,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
