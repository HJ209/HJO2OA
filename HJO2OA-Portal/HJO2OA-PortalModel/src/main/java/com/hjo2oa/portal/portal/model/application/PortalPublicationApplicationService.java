package com.hjo2oa.portal.portal.model.application;

import com.hjo2oa.portal.portal.model.domain.PortalModelContext;
import com.hjo2oa.portal.portal.model.domain.PortalModelContextProvider;
import com.hjo2oa.portal.portal.model.domain.PortalPublication;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationAudience;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationClientType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationIdentity;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationRepository;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PortalPublicationApplicationService {

    private final PortalPublicationRepository publicationRepository;
    private final PortalModelContextProvider contextProvider;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public PortalPublicationApplicationService(
            PortalPublicationRepository publicationRepository,
            PortalModelContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this(publicationRepository, contextProvider, domainEventPublisher, Clock.systemUTC());
    }
    public PortalPublicationApplicationService(
            PortalPublicationRepository publicationRepository,
            PortalModelContextProvider contextProvider,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.publicationRepository = Objects.requireNonNull(
                publicationRepository,
                "publicationRepository must not be null"
        );
        this.contextProvider = Objects.requireNonNull(contextProvider, "contextProvider must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public Optional<PortalPublicationView> current(String publicationId) {
        Objects.requireNonNull(publicationId, "publicationId must not be null");
        return publicationRepository.findByPublicationId(publicationId).map(PortalPublication::toView);
    }

    public List<PortalPublicationView> list(
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationStatus status
    ) {
        PortalModelContext context = contextProvider.currentContext();
        return publicationRepository.findAllByTenant(context.tenantId()).stream()
                .filter(publication -> sceneType == null || publication.sceneType() == sceneType)
                .filter(publication -> clientType == null || publication.clientType() == clientType)
                .filter(publication -> status == null || publication.status() == status)
                .map(PortalPublication::toView)
                .sorted(Comparator.comparing(PortalPublicationView::publicationId))
                .toList();
    }

    public Optional<PortalPublicationView> currentActive(
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType
    ) {
        return currentActive(sceneType, clientType, PortalPublicationIdentity.tenantDefault());
    }

    public Optional<PortalPublicationView> currentActive(
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationIdentity identity
    ) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(clientType, "clientType must not be null");
        Objects.requireNonNull(identity, "identity must not be null");
        PortalModelContext context = contextProvider.currentContext();
        return resolveActivePublication(
                context.tenantId(),
                sceneType,
                clientType,
                identity
        ).map(PortalPublication::toView);
    }

    public PortalPublicationView activate(ActivatePortalPublicationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PortalModelContext context = contextProvider.currentContext();
        Instant now = now();

        ensureActiveTargetUniqueness(
                context.tenantId(),
                command.sceneType(),
                command.clientType(),
                command.audience(),
                command.publicationId()
        );

        PortalPublication existing = publicationRepository.findByPublicationId(command.publicationId()).orElse(null);
        boolean changed = activationChanged(existing, command);
        PortalPublication publication = existing == null
                ? PortalPublication.create(
                        command.publicationId(),
                        context.tenantId(),
                        command.templateId(),
                        command.sceneType(),
                        command.clientType(),
                        command.audience(),
                        now
                )
                : existing.activate(
                        command.templateId(),
                        command.sceneType(),
                        command.clientType(),
                        command.audience(),
                        now
                );

        publicationRepository.save(publication);
        if (changed) {
            domainEventPublisher.publish(PortalPublicationActivatedEvent.from(publication, now));
        }
        return publication.toView();
    }

    public PortalPublicationView offline(OfflinePortalPublicationCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        PortalPublication publication = publicationRepository.findByPublicationId(command.publicationId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.RESOURCE_NOT_FOUND,
                        "Portal publication not found"
                ));
        if (publication.status() == PortalPublicationStatus.OFFLINE) {
            return publication.toView();
        }

        Instant now = now();
        PortalPublication offlinedPublication = publication.offline(now);
        publicationRepository.save(offlinedPublication);
        domainEventPublisher.publish(PortalPublicationOfflinedEvent.from(offlinedPublication, now));
        return offlinedPublication.toView();
    }

    private Instant now() {
        return clock.instant();
    }

    private void ensureActiveTargetUniqueness(
            String tenantId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationAudience audience,
            String publicationId
    ) {
        publicationRepository.findAllActiveByTenantAndSceneAndClient(tenantId, sceneType, clientType).stream()
                .filter(existing -> existing.audience().equals(audience))
                .filter(existing -> !existing.publicationId().equals(publicationId))
                .findFirst()
                .ifPresent(existing -> {
                    throw new BizException(
                            SharedErrorDescriptors.CONFLICT,
                            "Active publication already exists for scene, client type, and audience"
                    );
                });
    }

    private boolean activationChanged(PortalPublication existing, ActivatePortalPublicationCommand command) {
        if (existing == null) {
            return true;
        }
        if (existing.status() != PortalPublicationStatus.ACTIVE) {
            return true;
        }
        if (!existing.templateId().equals(command.templateId())) {
            return true;
        }
        if (existing.sceneType() != command.sceneType()) {
            return true;
        }
        if (existing.clientType() != command.clientType()) {
            return true;
        }
        return !existing.audience().equals(command.audience());
    }

    private Optional<PortalPublication> resolveActivePublication(
            String tenantId,
            PortalPublicationSceneType sceneType,
            PortalPublicationClientType clientType,
            PortalPublicationIdentity identity
    ) {
        List<PortalPublication> activePublications = publicationRepository.findAllActiveByTenantAndSceneAndClient(
                tenantId,
                sceneType,
                clientType
        );
        Map<PortalPublicationAudience, PortalPublication> publicationsByAudience = new LinkedHashMap<>();
        for (PortalPublication publication : activePublications) {
            publicationsByAudience.putIfAbsent(publication.audience(), publication);
        }
        return identity.candidateAudiences().stream()
                .map(publicationsByAudience::get)
                .filter(Objects::nonNull)
                .findFirst();
    }
}
