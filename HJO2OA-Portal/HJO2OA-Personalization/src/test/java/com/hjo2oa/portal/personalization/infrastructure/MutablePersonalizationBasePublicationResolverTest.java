package com.hjo2oa.portal.personalization.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import org.junit.jupiter.api.Test;

class MutablePersonalizationBasePublicationResolverTest {

    @Test
    void shouldResolveSeededBindingAndApplyOverride() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver();

        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.HOME))
                .isEqualTo("publication-home-default");

        resolver.bind(PersonalizationSceneType.HOME, "publication-home-v2");

        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.HOME))
                .isEqualTo("publication-home-v2");
    }

    @Test
    void shouldBootstrapFromActivePublicationProviderWhenNoBindingExists() {
        MutablePersonalizationBasePublicationResolver resolver =
                new MutablePersonalizationBasePublicationResolver(sceneType ->
                        sceneType == PersonalizationSceneType.HOME
                                ? java.util.Optional.of("publication-home-active")
                                : java.util.Optional.empty()
                );

        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.HOME))
                .isEqualTo("publication-home-active");
    }

    @Test
    void shouldPreferRuntimeResolvedPublicationOverSceneBindingWhenProviderIsConfigured() {
        MutablePersonalizationBasePublicationResolver resolver =
                new MutablePersonalizationBasePublicationResolver(
                        sceneType -> java.util.Optional.of("publication-home-active")
                );
        resolver.bind(PersonalizationSceneType.HOME, "publication-home-event");

        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.HOME))
                .isEqualTo("publication-home-active");
    }

    @Test
    void shouldFallBackToSeededBindingWhenMatchingOverrideIsRemoved() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver();
        resolver.bind(PersonalizationSceneType.OFFICE_CENTER, "publication-office-v2");

        resolver.unbind(PersonalizationSceneType.OFFICE_CENTER, "publication-office-v2");

        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.OFFICE_CENTER))
                .isEqualTo("publication-office-center-default");
    }

    @Test
    void shouldRemoveMatchingPublicationAcrossScenes() {
        MutablePersonalizationBasePublicationResolver resolver = new MutablePersonalizationBasePublicationResolver();
        resolver.bind(PersonalizationSceneType.HOME, "publication-shared");
        resolver.bind(PersonalizationSceneType.MOBILE_WORKBENCH, "publication-shared");

        resolver.unbindAll("publication-shared");

        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.HOME))
                .isEqualTo("publication-home-default");
        assertThat(resolver.resolveBasePublicationId(PersonalizationSceneType.MOBILE_WORKBENCH))
                .isEqualTo("publication-mobile-workbench-default");
    }
}
