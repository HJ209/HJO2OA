package com.hjo2oa.portal.portal.home.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeSceneType;
import com.hjo2oa.portal.portal.model.application.ActivatePortalPublicationCommand;
import com.hjo2oa.portal.portal.model.application.CreatePortalTemplateCommand;
import com.hjo2oa.portal.portal.model.application.PortalActiveTemplateResolutionApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.application.PublishPortalTemplateVersionCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplate;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalPublicationRepository;
import com.hjo2oa.portal.portal.model.infrastructure.InMemoryPortalTemplateRepository;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PortalModelBackedPortalHomePageTemplateProviderTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-04-20T14:00:00Z");

    @Test
    void shouldResolveTemplateMetadataFromPortalModelSourceState() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "office-default",
                "Office Source Template",
                PortalPublicationSceneType.OFFICE_CENTER
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.OFFICE_CENTER,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        PortalHomePageTemplate template = fixture.provider().templateFor(PortalHomeSceneType.OFFICE_CENTER);

        assertThat(template.branding().title()).isEqualTo("Office Source Template");
        assertThat(template.footer().text()).contains("template office-default", "page office-main", "publication publication-1");
        assertThat(template.sourceTemplateMetadata()).isNotNull();
        assertThat(template.sourceTemplateMetadata().templateCode()).isEqualTo("office-default");
        assertThat(template.sourceTemplateMetadata().publicationId()).isEqualTo("publication-1");
        assertThat(template.layoutType().name()).isEqualTo("OFFICE_SPLIT");
    }

    @Test
    void shouldFallBackToStaticTemplateWhenSourceStateIsMissing() {
        PortalModelBackedPortalHomePageTemplateProvider provider =
                new PortalModelBackedPortalHomePageTemplateProvider(
                        new PortalActiveTemplateResolutionApplicationService(
                                publicationApplicationService(),
                                templateApplicationService()
                        ),
                        new PortalTemplateCanvasApplicationService(new InMemoryPortalTemplateRepository()),
                        new StaticPortalHomePageTemplateProvider(),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                );

        PortalHomePageTemplate template = provider.templateFor(PortalHomeSceneType.HOME);

        assertThat(template.branding().title()).isEqualTo("HJO2OA Workspace");
        assertThat(template.footer().text()).contains("portal-home assembly layer");
        assertThat(template.sourceTemplateMetadata()).isNull();
    }

    @Test
    void shouldBuildTemplateStructureFromSourceCanvasWhenAvailable() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(fixture.templateRepository(), "template-1", List.of(new PortalPage(
                "page-home-custom",
                "home-custom",
                "Home Custom",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(
                        new PortalLayoutRegion(
                                "region-work-focus",
                                "work-focus",
                                "Work Focus",
                                true,
                                List.of(
                                        placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                        placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                                )
                        ),
                        new PortalLayoutRegion(
                                "region-identity-overview",
                                "identity-overview",
                                "Identity Overview",
                                true,
                                List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 30))
                        )
                )
        )));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));

        PortalHomePageTemplate template = fixture.provider().templateFor(PortalHomeSceneType.HOME);

        assertThat(template.branding().title()).isEqualTo("Home Source Template");
        assertThat(template.footer().text()).contains("page home-custom");
        assertThat(template.regions()).extracting(region -> region.regionCode())
                .containsExactly("work-focus", "identity-overview");
        assertThat(template.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("MESSAGE", "TODO");
        assertThat(template.regions().get(0).cards()).extracting(card -> card.title())
                .containsExactly("Unread Messages", "Pending Tasks");
        assertThat(template.regions().get(0).cards()).extracting(card -> card.sourcePlacementCode())
                .containsExactly("placement-message", "placement-todo");
        assertThat(template.regions().get(0).cards()).extracting(card -> card.sourceWidgetCode())
                .containsExactly("message-card", "todo-card");
    }

    @Test
    void shouldKeepPublishedStructureStableAfterDraftChangesUntilNextPublish() {
        TestFixture fixture = fixture();
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        overrideTemplatePages(fixture.templateRepository(), "template-1", publishedPages());
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        overrideTemplatePages(fixture.templateRepository(), "template-1", draftPages());

        PortalHomePageTemplate publishedTemplate = fixture.provider().templateFor(PortalHomeSceneType.HOME);

        assertThat(publishedTemplate.footer().text()).contains("page home-published");
        assertThat(publishedTemplate.regions()).extracting(region -> region.regionCode())
                .containsExactly("work-focus");
        assertThat(publishedTemplate.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("MESSAGE", "TODO");

        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-1", 2));

        PortalHomePageTemplate republishedTemplate = fixture.provider().templateFor(PortalHomeSceneType.HOME);

        assertThat(republishedTemplate.footer().text()).contains("page home-draft");
        assertThat(republishedTemplate.regions()).extracting(region -> region.regionCode())
                .containsExactly("identity-overview");
        assertThat(republishedTemplate.regions().get(0).cards()).extracting(card -> card.cardType().name())
                .containsExactly("IDENTITY");
    }

    @Test
    void shouldFallBackToStaticStructureWhenCanvasIsMissing() {
        InMemoryPortalTemplateRepository resolutionTemplateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(resolutionTemplateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        templateApplicationService.create(new CreatePortalTemplateCommand(
                "template-1",
                "home-default",
                "Home Source Template",
                PortalPublicationSceneType.HOME
        ));
        templateApplicationService.publish(new PublishPortalTemplateVersionCommand("template-1", 1));
        publicationApplicationService.activate(new ActivatePortalPublicationCommand(
                "publication-1",
                "template-1",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        PortalModelBackedPortalHomePageTemplateProvider provider =
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(new InMemoryPortalTemplateRepository()),
                        new StaticPortalHomePageTemplateProvider(),
                        () -> identityContext("assignment-1", "position-1", "person-1")
                );

        PortalHomePageTemplate template = provider.templateFor(PortalHomeSceneType.HOME);

        assertThat(template.branding().title()).isEqualTo("Home Source Template");
        assertThat(template.regions()).extracting(region -> region.regionCode())
                .containsExactly("identity-overview", "work-focus");
        assertThat(template.footer().text()).contains("template home-default", "publication publication-1");
        assertThat(template.sourceTemplateMetadata()).isNotNull();
    }

    @Test
    void shouldResolveDifferentPublicationsForDifferentIdentitiesInSameScene() {
        AtomicReference<PersonalizationIdentityContext> identity = new AtomicReference<>(
                identityContext("assignment-1", "position-1", "person-1")
        );
        TestFixture fixture = fixture(identity::get);
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-default",
                "home-default",
                "Home Default Template",
                PortalPublicationSceneType.HOME
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-default", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-home-default",
                "template-default",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.tenantDefault()
        ));
        fixture.templateApplicationService().create(new CreatePortalTemplateCommand(
                "template-assignment",
                "home-assignment",
                "Home Assignment Template",
                PortalPublicationSceneType.HOME
        ));
        fixture.templateApplicationService().publish(new PublishPortalTemplateVersionCommand("template-assignment", 1));
        fixture.publicationApplicationService().activate(new ActivatePortalPublicationCommand(
                "publication-home-assignment",
                "template-assignment",
                PortalPublicationSceneType.HOME,
                PortalPublicationClientType.PC,
                PortalPublicationAudience.ofAssignment("assignment-1")
        ));

        PortalHomePageTemplate assignmentTemplate = fixture.provider().templateFor(PortalHomeSceneType.HOME);

        identity.set(identityContext("assignment-2", "position-2", "person-2"));

        PortalHomePageTemplate defaultTemplate = fixture.provider().templateFor(PortalHomeSceneType.HOME);

        assertThat(assignmentTemplate.sourceTemplateMetadata()).isNotNull();
        assertThat(defaultTemplate.sourceTemplateMetadata()).isNotNull();
        assertThat(assignmentTemplate.sourceTemplateMetadata().publicationId())
                .isEqualTo("publication-home-assignment");
        assertThat(defaultTemplate.sourceTemplateMetadata().publicationId())
                .isEqualTo("publication-home-default");
        assertThat(assignmentTemplate.branding().title()).isEqualTo("Home Assignment Template");
        assertThat(defaultTemplate.branding().title()).isEqualTo("Home Default Template");
    }

    private TestFixture fixture() {
        return fixture(() -> identityContext("assignment-1", "position-1", "person-1"));
    }

    private TestFixture fixture(PersonalizationIdentityContextProvider identityContextProvider) {
        InMemoryPortalTemplateRepository templateRepository = new InMemoryPortalTemplateRepository();
        PortalTemplateApplicationService templateApplicationService = templateApplicationService(templateRepository);
        PortalPublicationApplicationService publicationApplicationService = publicationApplicationService();
        PortalActiveTemplateResolutionApplicationService resolutionApplicationService =
                new PortalActiveTemplateResolutionApplicationService(
                        publicationApplicationService,
                        templateApplicationService
                );
        return new TestFixture(
                templateRepository,
                templateApplicationService,
                publicationApplicationService,
                new PortalModelBackedPortalHomePageTemplateProvider(
                        resolutionApplicationService,
                        new PortalTemplateCanvasApplicationService(templateRepository),
                        new StaticPortalHomePageTemplateProvider(),
                        identityContextProvider
                )
        );
    }

    private PortalTemplateApplicationService templateApplicationService(InMemoryPortalTemplateRepository repository) {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        return new PortalTemplateApplicationService(
                repository,
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private PortalTemplateApplicationService templateApplicationService() {
        return templateApplicationService(new InMemoryPortalTemplateRepository());
    }

    private PortalPublicationApplicationService publicationApplicationService() {
        PortalModelContextProvider contextProvider = () -> new PortalModelContext("tenant-1", "portal-admin");
        return new PortalPublicationApplicationService(
                new InMemoryPortalPublicationRepository(),
                contextProvider,
                event -> {
                },
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
    }

    private void overrideTemplatePages(
            InMemoryPortalTemplateRepository repository,
            String templateId,
            List<PortalPage> pages
    ) {
        PortalTemplate existing = repository.findByTemplateId(templateId).orElseThrow();
        repository.save(new PortalTemplate(
                existing.templateId(),
                existing.tenantId(),
                existing.templateCode(),
                existing.displayName(),
                existing.sceneType(),
                pages,
                existing.versions(),
                existing.publishedSnapshots(),
                existing.createdAt(),
                FIXED_TIME.plusSeconds(60)
        ));
    }

    private List<PortalPage> publishedPages() {
        return List.of(new PortalPage(
                "page-home-published",
                "home-published",
                "Home Published",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new PortalLayoutRegion(
                        "region-work-focus",
                        "work-focus",
                        "Work Focus",
                        true,
                        List.of(
                                placement("placement-message", "message-card", WidgetCardType.MESSAGE, 10),
                                placement("placement-todo", "todo-card", WidgetCardType.TODO, 20)
                        )
                ))
        ));
    }

    private List<PortalPage> draftPages() {
        return List.of(new PortalPage(
                "page-home-draft",
                "home-draft",
                "Home Draft",
                true,
                PortalTemplateLayoutMode.THREE_SECTION,
                List.of(new PortalLayoutRegion(
                        "region-identity-overview",
                        "identity-overview",
                        "Identity Overview",
                        true,
                        List.of(placement("placement-identity", "identity-card", WidgetCardType.IDENTITY, 10))
                ))
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

    private PersonalizationIdentityContext identityContext(String assignmentId, String positionId, String personId) {
        return new PersonalizationIdentityContext("tenant-1", personId, assignmentId, positionId);
    }

    private record TestFixture(
            InMemoryPortalTemplateRepository templateRepository,
            PortalTemplateApplicationService templateApplicationService,
            PortalPublicationApplicationService publicationApplicationService,
            PortalModelBackedPortalHomePageTemplateProvider provider
    ) {
    }
}
