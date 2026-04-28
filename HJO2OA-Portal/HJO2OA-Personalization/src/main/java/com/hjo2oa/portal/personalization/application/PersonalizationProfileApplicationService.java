package com.hjo2oa.portal.personalization.application;

import com.hjo2oa.portal.personalization.domain.PersonalizationBasePublicationResolver;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContextProvider;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfile;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileKey;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileRepository;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileStatus;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileView;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationResetEvent;
import com.hjo2oa.portal.personalization.domain.PortalPersonalizationSavedEvent;
import com.hjo2oa.portal.personalization.domain.QuickAccessEntry;
import com.hjo2oa.portal.personalization.domain.ValidatedPersonalizationOverlay;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonalizationProfileApplicationService {

    private final PersonalizationProfileRepository profileRepository;
    private final PersonalizationIdentityContextProvider identityContextProvider;
    private final PersonalizationBasePublicationResolver basePublicationResolver;
    private final DomainEventPublisher domainEventPublisher;
    private final PersonalizationOverlaySaveValidator overlaySaveValidator;
    private final Clock clock;
    @Autowired
    public PersonalizationProfileApplicationService(
            PersonalizationProfileRepository profileRepository,
            PersonalizationIdentityContextProvider identityContextProvider,
            PersonalizationBasePublicationResolver basePublicationResolver,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                profileRepository,
                identityContextProvider,
                basePublicationResolver,
                domainEventPublisher,
                PersonalizationOverlaySaveValidator.noop(),
                Clock.systemUTC()
        );
    }
    public PersonalizationProfileApplicationService(
            PersonalizationProfileRepository profileRepository,
            PersonalizationIdentityContextProvider identityContextProvider,
            PersonalizationBasePublicationResolver basePublicationResolver,
            DomainEventPublisher domainEventPublisher,
            PersonalizationOverlaySaveValidator overlaySaveValidator
    ) {
        this(
                profileRepository,
                identityContextProvider,
                basePublicationResolver,
                domainEventPublisher,
                overlaySaveValidator,
                Clock.systemUTC()
        );
    }
    public PersonalizationProfileApplicationService(
            PersonalizationProfileRepository profileRepository,
            PersonalizationIdentityContextProvider identityContextProvider,
            PersonalizationBasePublicationResolver basePublicationResolver,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(
                profileRepository,
                identityContextProvider,
                basePublicationResolver,
                domainEventPublisher,
                PersonalizationOverlaySaveValidator.noop(),
                clock
        );
    }
    public PersonalizationProfileApplicationService(
            PersonalizationProfileRepository profileRepository,
            PersonalizationIdentityContextProvider identityContextProvider,
            PersonalizationBasePublicationResolver basePublicationResolver,
            DomainEventPublisher domainEventPublisher,
            PersonalizationOverlaySaveValidator overlaySaveValidator,
            Clock clock
    ) {
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository must not be null");
        this.identityContextProvider = Objects.requireNonNull(
                identityContextProvider,
                "identityContextProvider must not be null"
        );
        this.basePublicationResolver = Objects.requireNonNull(
                basePublicationResolver,
                "basePublicationResolver must not be null"
        );
        this.domainEventPublisher = Objects.requireNonNull(
                domainEventPublisher,
                "domainEventPublisher must not be null"
        );
        this.overlaySaveValidator = Objects.requireNonNull(
                overlaySaveValidator,
                "overlaySaveValidator must not be null"
        );
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PersonalizationProfileView current(PersonalizationSceneType sceneType) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        PersonalizationIdentityContext context = identityContextProvider.currentContext();
        return resolveCurrentView(context, sceneType, now());
    }

    public PersonalizationProfileView current(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext
    ) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(identityContext, "identityContext must not be null");
        return resolveCurrentView(identityContext, sceneType, now());
    }

    public PersonalizationProfileView save(SavePersonalizationProfileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.sceneType(), "sceneType must not be null");

        PersonalizationIdentityContext context = identityContextProvider.currentContext();
        String resolvedBasePublicationId = resolveBasePublicationId(command.sceneType(), context);
        PersonalizationProfileKey profileKey = resolveProfileKey(
                context,
                command.scope(),
                command.assignmentId(),
                command.sceneType()
        );
        Instant now = now();
        PersonalizationProfile existingProfile = profileRepository.findByKey(profileKey)
                .orElseGet(() -> PersonalizationProfile.create(
                        UUID.randomUUID().toString(),
                        context.tenantId(),
                        context.personId(),
                        profileKey.assignmentId(),
                        command.sceneType(),
                        resolvedBasePublicationId,
                        now
                ));
        ValidatedPersonalizationOverlay validatedOverlay = overlaySaveValidator.validate(
                command.sceneType(),
                context,
                resolvedBasePublicationId,
                existingProfile.basePublicationId(),
                command.widgetOrderOverride(),
                command.hiddenPlacementCodes()
        );

        PersonalizationProfile savedProfile = existingProfile.saveOverrides(
                normalizeOptional(command.themeCode()),
                normalizeCodeList(validatedOverlay.widgetOrderOverride(), "widgetOrderOverride"),
                normalizeCodeList(validatedOverlay.hiddenPlacementCodes(), "hiddenPlacementCodes"),
                normalizeQuickAccessEntries(command.quickAccessEntries()),
                now
        );
        profileRepository.save(savedProfile);
        domainEventPublisher.publish(PortalPersonalizationSavedEvent.from(savedProfile, now));
        return savedProfile.toView();
    }

    public PersonalizationProfileView reset(ResetPersonalizationProfileCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(command.sceneType(), "sceneType must not be null");

        PersonalizationIdentityContext context = identityContextProvider.currentContext();
        PersonalizationProfileKey profileKey = resolveProfileKey(
                context,
                command.scope(),
                command.assignmentId(),
                command.sceneType()
        );
        Instant now = now();
        String basePublicationId = resolveBasePublicationId(command.sceneType(), context);
        PersonalizationProfile existingProfile = profileRepository.findByKey(profileKey)
                .orElseGet(() -> PersonalizationProfile.create(
                        UUID.randomUUID().toString(),
                        context.tenantId(),
                        context.personId(),
                        profileKey.assignmentId(),
                        command.sceneType(),
                        basePublicationId,
                        now
                ));
        PersonalizationProfile resetProfile = existingProfile.reset(basePublicationId, now);
        profileRepository.save(resetProfile);
        domainEventPublisher.publish(PortalPersonalizationResetEvent.from(resetProfile, now));
        return resetProfile.toView();
    }

    private Instant now() {
        return clock.instant();
    }

    private PersonalizationProfileView resolveCurrentView(
            PersonalizationIdentityContext context,
            PersonalizationSceneType sceneType,
            Instant resolvedAt
    ) {
        Optional<PersonalizationProfile> assignmentProfile = profileRepository.findByKey(
                PersonalizationProfileKey.ofAssignment(
                        context.tenantId(),
                        context.personId(),
                        context.assignmentId(),
                        sceneType
                )
        );
        if (assignmentProfile.isPresent()) {
            return assignmentProfile.orElseThrow().toView();
        }

        Optional<PersonalizationProfile> globalProfile = profileRepository.findByKey(
                PersonalizationProfileKey.ofGlobal(context.tenantId(), context.personId(), sceneType)
        );
        if (globalProfile.isPresent()) {
            return globalProfile.orElseThrow().toView();
        }

        return new PersonalizationProfileView(
                null,
                context.tenantId(),
                context.personId(),
                context.assignmentId(),
                sceneType,
                PersonalizationProfileScope.ASSIGNMENT,
                resolveBasePublicationId(sceneType, context),
                null,
                List.of(),
                List.of(),
                List.of(),
                PersonalizationProfileStatus.RESET,
                resolvedAt,
                null,
                null
        );
    }

    private PersonalizationProfileKey resolveProfileKey(
            PersonalizationIdentityContext context,
            PersonalizationProfileScope requestedScope,
            String requestedAssignmentId,
            PersonalizationSceneType sceneType
    ) {
        PersonalizationProfileScope scope = requestedScope == null
                ? PersonalizationProfileScope.ASSIGNMENT
                : requestedScope;
        if (scope == PersonalizationProfileScope.ASSIGNMENT) {
            String normalizedAssignmentId = normalizeOptional(requestedAssignmentId);
            if (normalizedAssignmentId != null && !normalizedAssignmentId.equals(context.assignmentId())) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "assignmentId does not match current identity"
                );
            }
            return PersonalizationProfileKey.ofAssignment(
                    context.tenantId(),
                    context.personId(),
                    context.assignmentId(),
                    sceneType
            );
        }

        if (normalizeOptional(requestedAssignmentId) != null) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "global personalization must not specify assignmentId"
            );
        }
        return PersonalizationProfileKey.ofGlobal(context.tenantId(), context.personId(), sceneType);
    }

    private String resolveBasePublicationId(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext
    ) {
        return Objects.requireNonNull(
                identityContext == null
                        ? basePublicationResolver.resolveBasePublicationId(sceneType)
                        : basePublicationResolver.resolveBasePublicationId(sceneType, identityContext),
                "basePublicationResolver must not return null"
        );
    }

    private List<String> normalizeCodeList(List<String> values, String fieldName) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalizedValues = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + " must not contain null");
            }
            String normalizedValue = normalizeRequired(value, fieldName);
            if (!normalizedValues.add(normalizedValue)) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "duplicate " + fieldName + " placement code: " + normalizedValue
                );
            }
        }
        return List.copyOf(normalizedValues);
    }

    private List<QuickAccessEntry> normalizeQuickAccessEntries(List<QuickAccessEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, QuickAccessEntry> normalizedEntries = new LinkedHashMap<>();
        for (QuickAccessEntry entry : entries) {
            Objects.requireNonNull(entry, "quickAccessEntries must not contain null");
            String duplicateKey = entry.entryType().name() + ":" + entry.targetCode();
            if (normalizedEntries.putIfAbsent(duplicateKey, entry) != null) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "duplicate quick access target: " + entry.targetCode()
                );
            }
        }
        return List.copyOf(normalizedEntries.values());
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldName + " must not contain blank value");
        }
        return normalized;
    }
}
