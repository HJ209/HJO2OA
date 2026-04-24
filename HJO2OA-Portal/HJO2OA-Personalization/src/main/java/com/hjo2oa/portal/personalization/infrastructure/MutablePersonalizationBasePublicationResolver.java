package com.hjo2oa.portal.personalization.infrastructure;

import com.hjo2oa.portal.personalization.domain.PersonalizationActivePublicationProvider;
import com.hjo2oa.portal.personalization.domain.PersonalizationBasePublicationBindingManager;
import com.hjo2oa.portal.personalization.domain.PersonalizationBasePublicationResolver;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MutablePersonalizationBasePublicationResolver implements
        PersonalizationBasePublicationResolver,
        PersonalizationBasePublicationBindingManager {

    private static final PersonalizationActivePublicationProvider NO_OP_ACTIVE_PUBLICATION_PROVIDER =
            sceneType -> Optional.empty();

    private final Map<PersonalizationSceneType, String> seededBindings = Map.of(
            PersonalizationSceneType.HOME, "publication-home-default",
            PersonalizationSceneType.OFFICE_CENTER, "publication-office-center-default",
            PersonalizationSceneType.MOBILE_WORKBENCH, "publication-mobile-workbench-default"
    );
    private final Map<PersonalizationSceneType, String> activeBindings = new ConcurrentHashMap<>();
    private PersonalizationActivePublicationProvider activePublicationProvider = NO_OP_ACTIVE_PUBLICATION_PROVIDER;

    public MutablePersonalizationBasePublicationResolver() {
    }

    public MutablePersonalizationBasePublicationResolver(
            PersonalizationActivePublicationProvider activePublicationProvider
    ) {
        setActivePublicationProvider(activePublicationProvider);
    }

    @Override
    public String resolveBasePublicationId(PersonalizationSceneType sceneType) {
        return resolveBasePublicationId(sceneType, null);
    }

    @Override
    public String resolveBasePublicationId(
            PersonalizationSceneType sceneType,
            com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext identityContext
    ) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Optional<String> runtimeResolvedPublication = resolveRuntimePublication(sceneType, identityContext)
                .map(publicationId -> requireText(publicationId, "publicationId"));
        // Scene-level event bindings are only safe before an identity-aware runtime provider is available.
        if (hasRuntimePublicationProvider()) {
            return runtimeResolvedPublication.orElse(seededBindings.get(sceneType));
        }
        String activeBinding = activeBindings.get(sceneType);
        if (activeBinding != null) {
            return activeBinding;
        }
        return runtimeResolvedPublication.orElse(seededBindings.get(sceneType));
    }

    private Optional<String> resolveRuntimePublication(
            PersonalizationSceneType sceneType,
            com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext identityContext
    ) {
        return identityContext == null
                ? activePublicationProvider.findActivePublicationId(sceneType)
                : activePublicationProvider.findActivePublicationId(sceneType, identityContext);
    }

    @Override
    public void bind(PersonalizationSceneType sceneType, String publicationId) {
        activeBindings.put(
                Objects.requireNonNull(sceneType, "sceneType must not be null"),
                requireText(publicationId, "publicationId")
        );
    }

    @Override
    public void unbind(PersonalizationSceneType sceneType, String publicationId) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        String normalizedPublicationId = requireText(publicationId, "publicationId");
        activeBindings.computeIfPresent(sceneType, (key, currentValue) ->
                currentValue.equals(normalizedPublicationId) ? null : currentValue
        );
    }

    @Override
    public void unbindAll(String publicationId) {
        String normalizedPublicationId = requireText(publicationId, "publicationId");
        activeBindings.entrySet().removeIf(entry -> entry.getValue().equals(normalizedPublicationId));
    }

    @Autowired(required = false)
    void setActivePublicationProvider(PersonalizationActivePublicationProvider activePublicationProvider) {
        this.activePublicationProvider = Objects.requireNonNull(
                activePublicationProvider,
                "activePublicationProvider must not be null"
        );
    }

    private boolean hasRuntimePublicationProvider() {
        return activePublicationProvider != NO_OP_ACTIVE_PUBLICATION_PROVIDER;
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
