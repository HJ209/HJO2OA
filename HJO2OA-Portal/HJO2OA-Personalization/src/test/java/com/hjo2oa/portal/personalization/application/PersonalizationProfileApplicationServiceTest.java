package com.hjo2oa.portal.personalization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hjo2oa.portal.personalization.domain.PersonalizationBasePublicationResolver;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfile;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileView;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationResetEvent;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.personalization.domain.QuickAccessEntry;
import com.hjo2oa.portal.personalization.domain.QuickAccessEntryType;
import com.hjo2oa.portal.personalization.infrastructure.InMemoryPersonalizationProfileRepository;
import com.hjo2oa.portal.personalization.infrastructure.MutablePersonalizationBasePublicationResolver;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.widget.config.application.UpsertWidgetDefinitionCommand;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import com.hjo2oa.portal.widget.config.domain.WidgetDataSourceType;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.portal.widget.config.infrastructure.InMemoryWidgetDefinitionRepository;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.messaging.DomainEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PersonalizationProfileApplicationServiceTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T02:00:00Z");

    @Test
    void shouldSaveAssignmentScopedProfileAndPublishSavedEvent() {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PersonalizationProfileApplicationService applicationService = applicationService(repository, publishedEvents);

        PersonalizationProfileView savedProfile = applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.OFFICE_CENTER,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                "office-red",
                List.of("todo-card", "message-card"),
                List.of("announcement-card"),
                List.of(new QuickAccessEntry(
                        QuickAccessEntryType.PROCESS,
                        "leave.apply",
                        null,
                        "calendar",
                        10,
                        true
                ))
        ));

        assertThat(savedProfile.profileId()).isNotBlank();
        assertThat(savedProfile.sceneType()).isEqualTo(PersonalizationSceneType.OFFICE_CENTER);
        assertThat(savedProfile.resolvedScope()).isEqualTo(PersonalizationProfileScope.ASSIGNMENT);
        assertThat(savedProfile.assignmentId()).isEqualTo("assignment-1");
        assertThat(savedProfile.themeCode()).isEqualTo("office-red");
        assertThat(savedProfile.quickAccessEntries()).hasSize(1);
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalPersonalizationSavedEvent.class);
        PortalPersonalizationSavedEvent savedEvent = (PortalPersonalizationSavedEvent) publishedEvents.get(0);
        assertThat(savedEvent.sceneType()).isEqualTo(PersonalizationSceneType.OFFICE_CENTER);
        assertThat(savedEvent.personId()).isEqualTo("person-1");
    }

    @Test
    void shouldFallbackToGlobalProfileWhenAssignmentScopedProfileIsMissing() {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        PersonalizationProfileApplicationService applicationService = applicationService(repository, new ArrayList<>());

        applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.GLOBAL,
                null,
                "global-light",
                List.of("todo-card"),
                List.of(),
                List.of()
        ));

        PersonalizationProfileView currentProfile = applicationService.current(PersonalizationSceneType.HOME);

        assertThat(currentProfile.resolvedScope()).isEqualTo(PersonalizationProfileScope.GLOBAL);
        assertThat(currentProfile.assignmentId()).isNull();
        assertThat(currentProfile.themeCode()).isEqualTo("global-light");
    }

    @Test
    void shouldResetProfileAndPublishResetEvent() {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PersonalizationProfileApplicationService applicationService = applicationService(repository, publishedEvents);
        applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.MOBILE_WORKBENCH,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                "mobile-blue",
                List.of("message-card"),
                List.of("content-card"),
                List.of(new QuickAccessEntry(
                        QuickAccessEntryType.APP,
                        "mobile.portal",
                        null,
                        null,
                        1,
                        false
                ))
        ));
        publishedEvents.clear();

        PersonalizationProfileView resetProfile = applicationService.reset(new ResetPersonalizationProfileCommand(
                PersonalizationSceneType.MOBILE_WORKBENCH,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1"
        ));

        assertThat(resetProfile.status().name()).isEqualTo("RESET");
        assertThat(resetProfile.themeCode()).isNull();
        assertThat(resetProfile.widgetOrderOverride()).isEmpty();
        assertThat(resetProfile.hiddenPlacementCodes()).isEmpty();
        assertThat(resetProfile.quickAccessEntries()).isEmpty();
        assertThat(publishedEvents).singleElement().isInstanceOf(PortalPersonalizationResetEvent.class);
    }

    @Test
    void shouldUseUpdatedBasePublicationBindingWhenResolvingCurrentView() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver();
        resolver.bind(PersonalizationSceneType.OFFICE_CENTER, "publication-office-v2");
        PersonalizationProfileApplicationService applicationService = applicationService(
                new InMemoryPersonalizationProfileRepository(),
                new ArrayList<>(),
                resolver
        );

        PersonalizationProfileView currentProfile = applicationService.current(PersonalizationSceneType.OFFICE_CENTER);

        assertThat(currentProfile.basePublicationId()).isEqualTo("publication-office-v2");
    }

    @Test
    void shouldUseBootstrappedBasePublicationWhenCurrentProfileIsMissing() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver(
                sceneType -> sceneType == PersonalizationSceneType.HOME
                        ? java.util.Optional.of("publication-home-active")
                        : java.util.Optional.empty()
        );
        PersonalizationProfileApplicationService applicationService = applicationService(
                new InMemoryPersonalizationProfileRepository(),
                new ArrayList<>(),
                resolver
        );

        PersonalizationProfileView currentProfile = applicationService.current(PersonalizationSceneType.HOME);

        assertThat(currentProfile.basePublicationId()).isEqualTo("publication-home-active");
    }

    @Test
    void shouldUseBootstrappedBasePublicationWhenCreatingProfile() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver(
                sceneType -> sceneType == PersonalizationSceneType.HOME
                        ? java.util.Optional.of("publication-home-active")
                        : java.util.Optional.empty()
        );
        PersonalizationProfileApplicationService applicationService = applicationService(
                new InMemoryPersonalizationProfileRepository(),
                new ArrayList<>(),
                resolver
        );

        PersonalizationProfileView savedProfile = applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                null,
                List.of(),
                List.of(),
                List.of()
        ));

        assertThat(savedProfile.basePublicationId()).isEqualTo("publication-home-active");
    }

    @Test
    void shouldRebaseToBootstrappedBasePublicationWhenResettingProfile() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver(
                sceneType -> sceneType == PersonalizationSceneType.MOBILE_WORKBENCH
                        ? java.util.Optional.of("publication-mobile-active")
                        : java.util.Optional.empty()
        );
        PersonalizationProfileApplicationService applicationService = applicationService(
                new InMemoryPersonalizationProfileRepository(),
                new ArrayList<>(),
                resolver
        );
        applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.MOBILE_WORKBENCH,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                "mobile-blue",
                List.of("message-card"),
                List.of("content-card"),
                List.of()
        ));

        PersonalizationProfileView resetProfile = applicationService.reset(new ResetPersonalizationProfileCommand(
                PersonalizationSceneType.MOBILE_WORKBENCH,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1"
        ));

        assertThat(resetProfile.basePublicationId()).isEqualTo("publication-mobile-active");
        assertThat(resetProfile.status()).isEqualTo(com.hjo2oa.portal.personalization.domain.PersonalizationProfileStatus.RESET);
    }

    @Test
    void shouldRejectCrossIdentityAssignmentSave() {
        PersonalizationProfileApplicationService applicationService = applicationService(
                new InMemoryPersonalizationProfileRepository(),
                new ArrayList<>()
        );

        assertThatThrownBy(() -> applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-2",
                null,
                List.of(),
                List.of(),
                List.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("assignmentId does not match current identity");
    }

    @Test
    void shouldUseCurrentIdentityContextWhenCommandDoesNotSpecifyAssignment() {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PersonalizationIdentityContext identityContext = new PersonalizationIdentityContext(
                "tenant-9",
                "person-9",
                "assignment-9",
                "position-9"
        );
        PersonalizationProfileApplicationService applicationService = applicationService(
                repository,
                publishedEvents,
                new MutablePersonalizationBasePublicationResolver(),
                identityContext
        );

        PersonalizationProfileView savedProfile = applicationService.save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                null,
                null,
                "context-bound",
                List.of("message-card", "todo-card"),
                List.of(),
                List.of()
        ));
        PersonalizationProfileView currentProfile = applicationService.current(PersonalizationSceneType.HOME);
        PersonalizationProfileView resetProfile = applicationService.reset(new ResetPersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                null,
                null
        ));

        assertThat(savedProfile.tenantId()).isEqualTo("tenant-9");
        assertThat(savedProfile.personId()).isEqualTo("person-9");
        assertThat(savedProfile.assignmentId()).isEqualTo("assignment-9");
        assertThat(currentProfile.assignmentId()).isEqualTo("assignment-9");
        assertThat(resetProfile.assignmentId()).isEqualTo("assignment-9");
        assertThat(publishedEvents).extracting(DomainEvent::eventType)
                .containsExactly(
                        PortalPersonalizationSavedEvent.EVENT_TYPE,
                        PortalPersonalizationResetEvent.EVENT_TYPE
                );
        assertThat(((PortalPersonalizationSavedEvent) publishedEvents.get(0)).personId()).isEqualTo("person-9");
    }

    @Test
    void shouldResolveCurrentProfileUsingExplicitIdentityContext() {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        repository.save(
                PersonalizationProfile.create(
                                "profile-explicit",
                                "tenant-2",
                                "person-2",
                                "assignment-2",
                                PersonalizationSceneType.HOME,
                                "publication-home-position-2",
                                FIXED_TIME
                        )
                        .saveOverrides(
                                "explicit-theme",
                                List.of("message-card"),
                                List.of("placement-message"),
                                List.of(),
                                FIXED_TIME.plusSeconds(60)
                        )
        );
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver() {
            @Override
            public String resolveBasePublicationId(
                    PersonalizationSceneType sceneType,
                    PersonalizationIdentityContext identityContext
            ) {
                return "publication-home-" + identityContext.positionId();
            }
        };
        PersonalizationProfileApplicationService applicationService = applicationService(
                repository,
                new ArrayList<>(),
                resolver
        );

        PersonalizationProfileView currentProfile = applicationService.current(
                PersonalizationSceneType.HOME,
                new PersonalizationIdentityContext("tenant-2", "person-2", "assignment-2", "position-2")
        );

        assertThat(currentProfile.assignmentId()).isEqualTo("assignment-2");
        assertThat(currentProfile.themeCode()).isEqualTo("explicit-theme");
        assertThat(currentProfile.basePublicationId()).isEqualTo("publication-home-position-2");
    }

    @Test
    void shouldSaveOverlayAgainstResolvedLivePublicationPlacements() {
        ValidationFixture fixture = validationFixture(false);

        PersonalizationProfileView savedProfile = fixture.applicationService().save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                "validated-home",
                List.of("message-card", "placement-todo"),
                List.of("identity-card"),
                List.of()
        ));

        assertThat(savedProfile.basePublicationId()).isEqualTo("publication-home-v1");
        assertThat(savedProfile.widgetOrderOverride()).containsExactly("placement-message", "placement-todo");
        assertThat(savedProfile.hiddenPlacementCodes()).containsExactly("placement-identity");
    }

    @Test
    void shouldRejectUnknownLivePlacementReferenceOnSave() {
        ValidationFixture fixture = validationFixture(false);

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                null,
                List.of("message-card"),
                List.of("placement-ghost"),
                List.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("placement-ghost");
    }

    @Test
    void shouldRejectHidingRequiredPlacementOnSave() {
        ValidationFixture fixture = validationFixture(true);

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                null,
                List.of(),
                List.of("placement-identity"),
                List.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("placement-identity")
                .hasMessageContaining("required region");
    }

    @Test
    void shouldRejectSaveWhenExistingProfilePublicationBecomesStale() {
        ValidationFixture fixture = validationFixture(false);
        fixture.applicationService().save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                null,
                List.of("message-card"),
                List.of(),
                List.of()
        ));
        fixture.basePublicationResolver().bind(PersonalizationSceneType.HOME, "publication-home-v2");

        assertThatThrownBy(() -> fixture.applicationService().save(new SavePersonalizationProfileCommand(
                PersonalizationSceneType.HOME,
                PersonalizationProfileScope.ASSIGNMENT,
                "assignment-1",
                "updated-theme",
                List.of("message-card"),
                List.of(),
                List.of()
        )))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("publication-home-v1")
                .hasMessageContaining("publication-home-v2");
    }

    private ValidationFixture validationFixture(boolean requiredIdentityRegion) {
        InMemoryPersonalizationProfileRepository repository = new InMemoryPersonalizationProfileRepository();
        List<DomainEvent> publishedEvents = new ArrayList<>();
        PersonalizationIdentityContext identityContext = new PersonalizationIdentityContext(
                "tenant-1",
                "person-1",
                "assignment-1",
                "position-1"
        );
        MutablePersonalizationBasePublicationResolver basePublicationResolver =
                new MutablePersonalizationBasePublicationResolver();
        PortalModelContextProvider portalContextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        WidgetConfigContextProvider widgetContextProvider = () -> new WidgetConfigContext("tenant-1", "widget-admin");
        InMemoryPortalPublicationRepository publicationRepository = new InMemoryPortalPublicationRepository();
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        InMemoryWidgetDefinitionRepository widgetRepository = new InMemoryWidgetDefinitionRepository();
        PortalPublicationApplicationService publicationApplicationService = new PortalPublicationApplicationService(
                publicationRepository,
                portalContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalTemplateApplicationService templateApplicationService = new PortalTemplateApplicationService(
                templateRepository,
                portalContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        PortalTemplateCanvasApplicationService canvasApplicationService = new PortalTemplateCanvasApplicationService(
                templateRepository,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        WidgetDefinitionApplicationService widgetDefinitionApplicationService = new WidgetDefinitionApplicationService(
                widgetRepository,
                widgetContextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        upsertWidgetDefinition(
                widgetDefinitionApplicationService,
                "widget-message",
                "message-card",
                WidgetCardType.MESSAGE
        );
        upsertWidgetDefinition(
                widgetDefinitionApplicationService,
                "widget-todo",
                "todo-card",
                WidgetCardType.TODO
        );
        upsertWidgetDefinition(
                widgetDefinitionApplicationService,
                "widget-identity",
                "identity-card",
                WidgetCardType.IDENTITY
        );
        createPublishedTemplate(
                templateApplicationService,
                canvasApplicationService,
                publicationApplicationService,
                "template-home-v1",
                "home-template-v1",
                "publication-home-v1",
                PortalPublicationAudience.tenantDefault(),
                requiredIdentityRegion
        );
        createPublishedTemplate(
                templateApplicationService,
                canvasApplicationService,
                publicationApplicationService,
                "template-home-v2",
                "home-template-v2",
                "publication-home-v2",
                PortalPublicationAudience.ofPosition("position-1"),
                requiredIdentityRegion
        );
        basePublicationResolver.bind(PersonalizationSceneType.HOME, "publication-home-v1");
        PersonalizationOverlaySaveValidator overlaySaveValidator = new PortalModelPersonalizationOverlaySaveValidator(
                publicationApplicationService,
                canvasApplicationService,
                widgetDefinitionApplicationService
        );
        PersonalizationProfileApplicationService applicationService = new PersonalizationProfileApplicationService(
                repository,
                () -> identityContext,
                basePublicationResolver,
                publishedEvents::add,
                overlaySaveValidator,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return new ValidationFixture(applicationService, basePublicationResolver);
    }

    private void createPublishedTemplate(
            PortalTemplateApplicationService templateApplicationService,
            PortalTemplateCanvasApplicationService canvasApplicationService,
            PortalPublicationApplicationService publicationApplicationService,
            String templateId,
            String templateCode,
            String publicationId,
            PortalPublicationAudience audience,
            boolean requiredIdentityRegion
    ) {
        templateApplicationService.create(new CreatePortalTemplateCommand(
                templateId,
                templateCode,
                templateCode,
                PortalPublicationSceneType.HOME
        ));
        canvasApplicationService.save(new SavePortalTemplateCanvasCommand(
                templateId,
                publishedPages(requiredIdentityRegion)
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand(templateId, 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                publicationId,
                templateId,
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                audience
        ));
    }

    private List<PortalPage> publishedPages(boolean requiredIdentityRegion) {
        return List.of(new PortalPage(
                "page-home-default",
                "home-default",
                "Home Default",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(
                        new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                false,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                                )
                        ),
                        new PortalLayoutRegion(
                                "region-identity",
                                "identity-overview",
                                "Identity Overview",
                                requiredIdentityRegion,
                                List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30))
                        )
                )
        ));
    }

    private PortalWidgetPlacement placement(
            String placementCode,
            String widgetCode,
            WidgetCardType cardType,
            int orderNo
    ) {
        return new PortalWidgetPlacement(
                placementCode,
                placementCode,
                widgetCode,
                cardType,
                orderNo,
                false,
                false,
                Map.of()
        );
    }

    private void upsertWidgetDefinition(
            WidgetDefinitionApplicationService widgetDefinitionApplicationService,
            String widgetId,
            String widgetCode,
            WidgetCardType cardType
    ) {
        widgetDefinitionApplicationService.upsert(new UpsertWidgetDefinitionCommand(
                widgetId,
                widgetCode,
                widgetCode,
                cardType,
                WidgetSceneType.HOME,
                "aggregation-api",
                WidgetDataSourceType.AGGREGATION_QUERY,
                true,
                true,
                8
        ));
    }

    private record ValidationFixture(
            PersonalizationProfileApplicationService applicationService,
            MutablePersonalizationBasePublicationResolver basePublicationResolver
    ) {
    }

    private PersonalizationProfileApplicationService applicationService(
            InMemoryPersonalizationProfileRepository repository,
            List<DomainEvent> publishedEvents
    ) {
        return applicationService(repository, publishedEvents, new MutablePersonalizationBasePublicationResolver());
    }

    private PersonalizationProfileApplicationService applicationService(
            InMemoryPersonalizationProfileRepository repository,
            List<DomainEvent> publishedEvents,
            PersonalizationBasePublicationResolver basePublicationResolver
    ) {
        return applicationService(
                repository,
                publishedEvents,
                basePublicationResolver,
                new PersonalizationIdentityContext("tenant-1", "person-1", "assignment-1", "position-1")
        );
    }

    private PersonalizationProfileApplicationService applicationService(
            InMemoryPersonalizationProfileRepository repository,
            List<DomainEvent> publishedEvents,
            PersonalizationBasePublicationResolver basePublicationResolver,
            PersonalizationIdentityContext identityContext
    ) {
        PersonalizationIdentityContextProvider identityContextProvider = () -> identityContext;
        return new PersonalizationProfileApplicationService(
                repository,
                identityContextProvider,
                basePublicationResolver,
                publishedEvents::add,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }
}
