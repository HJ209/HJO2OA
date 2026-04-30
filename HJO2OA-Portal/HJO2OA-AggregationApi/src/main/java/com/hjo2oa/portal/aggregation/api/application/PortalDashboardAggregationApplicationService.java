package com.hjo2oa.portal.aggregation.api.application;

import com.hjo2oa.portal.aggregation.api.domain.PortalAggregationSnapshotKey;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshot;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardSnapshotRepository;
import com.hjo2oa.portal.aggregation.api.domain.PortalCardType;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalContentCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalDashboardView;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalIdentityCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalMessageCardDataProvider;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotFailedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSnapshotRefreshedEvent;
import com.hjo2oa.portal.aggregation.api.domain.PortalSceneType;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCard;
import com.hjo2oa.portal.aggregation.api.domain.PortalTodoCardDataProvider;
import com.hjo2oa.shared.messaging.DomainEventPublisher;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class PortalDashboardAggregationApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PortalDashboardAggregationApplicationService.class);
    private static final String TODO_CARD_DEGRADED = "Todo card is temporarily unavailable";
    private static final String MESSAGE_CARD_DEGRADED = "Message card is temporarily unavailable";
    private static final String CONTENT_CARD_DEGRADED = "Content card is temporarily unavailable";

    private final PortalIdentityCardDataProvider identityCardDataProvider;
    private final PortalTodoCardDataProvider todoCardDataProvider;
    private final PortalMessageCardDataProvider messageCardDataProvider;
    private final PortalContentCardDataProvider contentCardDataProvider;
    private final PortalCardSnapshotRepository snapshotRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;
    @Autowired
    public PortalDashboardAggregationApplicationService(
            PortalIdentityCardDataProvider identityCardDataProvider,
            PortalTodoCardDataProvider todoCardDataProvider,
            PortalMessageCardDataProvider messageCardDataProvider,
            PortalContentCardDataProvider contentCardDataProvider,
            PortalCardSnapshotRepository snapshotRepository
    ) {
        this(
                identityCardDataProvider,
                todoCardDataProvider,
                messageCardDataProvider,
                contentCardDataProvider,
                snapshotRepository,
                event -> {
                },
                Clock.systemUTC()
        );
    }
    public PortalDashboardAggregationApplicationService(
            PortalIdentityCardDataProvider identityCardDataProvider,
            PortalTodoCardDataProvider todoCardDataProvider,
            PortalMessageCardDataProvider messageCardDataProvider,
            PortalCardSnapshotRepository snapshotRepository
    ) {
        this(
                identityCardDataProvider,
                todoCardDataProvider,
                messageCardDataProvider,
                PortalContentCard::empty,
                snapshotRepository,
                event -> {
                },
                Clock.systemUTC()
        );
    }
    public PortalDashboardAggregationApplicationService(
            PortalIdentityCardDataProvider identityCardDataProvider,
            PortalTodoCardDataProvider todoCardDataProvider,
            PortalMessageCardDataProvider messageCardDataProvider,
            PortalCardSnapshotRepository snapshotRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                identityCardDataProvider,
                todoCardDataProvider,
                messageCardDataProvider,
                PortalContentCard::empty,
                snapshotRepository,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }
    public PortalDashboardAggregationApplicationService(
            PortalIdentityCardDataProvider identityCardDataProvider,
            PortalTodoCardDataProvider todoCardDataProvider,
            PortalMessageCardDataProvider messageCardDataProvider,
            PortalContentCardDataProvider contentCardDataProvider,
            PortalCardSnapshotRepository snapshotRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this(
                identityCardDataProvider,
                todoCardDataProvider,
                messageCardDataProvider,
                contentCardDataProvider,
                snapshotRepository,
                domainEventPublisher,
                Clock.systemUTC()
        );
    }
    public PortalDashboardAggregationApplicationService(
            PortalIdentityCardDataProvider identityCardDataProvider,
            PortalTodoCardDataProvider todoCardDataProvider,
            PortalMessageCardDataProvider messageCardDataProvider,
            PortalCardSnapshotRepository snapshotRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this(
                identityCardDataProvider,
                todoCardDataProvider,
                messageCardDataProvider,
                PortalContentCard::empty,
                snapshotRepository,
                domainEventPublisher,
                clock
        );
    }
    public PortalDashboardAggregationApplicationService(
            PortalIdentityCardDataProvider identityCardDataProvider,
            PortalTodoCardDataProvider todoCardDataProvider,
            PortalMessageCardDataProvider messageCardDataProvider,
            PortalContentCardDataProvider contentCardDataProvider,
            PortalCardSnapshotRepository snapshotRepository,
            DomainEventPublisher domainEventPublisher,
            Clock clock
    ) {
        this.identityCardDataProvider = Objects.requireNonNull(identityCardDataProvider, "identityCardDataProvider must not be null");
        this.todoCardDataProvider = Objects.requireNonNull(todoCardDataProvider, "todoCardDataProvider must not be null");
        this.messageCardDataProvider = Objects.requireNonNull(messageCardDataProvider, "messageCardDataProvider must not be null");
        this.contentCardDataProvider = Objects.requireNonNull(contentCardDataProvider, "contentCardDataProvider must not be null");
        this.snapshotRepository = Objects.requireNonNull(snapshotRepository, "snapshotRepository must not be null");
        this.domainEventPublisher = Objects.requireNonNull(domainEventPublisher, "domainEventPublisher must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public PortalDashboardView dashboard(PortalSceneType sceneType, Set<PortalCardType> requestedCards) {
        PortalSceneType resolvedSceneType = sceneType == null ? PortalSceneType.HOME : sceneType;
        PortalIdentityCard identityCard = identityCardDataProvider.currentIdentity();
        PortalCardSnapshot<PortalIdentityCard> identitySnapshot = resolveIdentitySnapshot(identityCard, resolvedSceneType);
        Set<PortalCardType> normalizedCards = normalizeRequestedCards(requestedCards);

        PortalCardSnapshot<PortalTodoCard> todoSnapshot = normalizedCards.contains(PortalCardType.TODO)
                ? readPortalCard(
                identityCard,
                resolvedSceneType,
                PortalCardType.TODO,
                todoCardDataProvider::currentTodoCard,
                PortalTodoCard::empty,
                TODO_CARD_DEGRADED)
                : null;

        PortalCardSnapshot<PortalMessageCard> messageSnapshot = normalizedCards.contains(PortalCardType.MESSAGE)
                ? readPortalCard(
                identityCard,
                resolvedSceneType,
                PortalCardType.MESSAGE,
                messageCardDataProvider::currentMessageCard,
                PortalMessageCard::empty,
                MESSAGE_CARD_DEGRADED)
                : null;

        PortalCardSnapshot<PortalContentCard> contentSnapshot = normalizedCards.contains(PortalCardType.CONTENT)
                ? readPortalCard(
                identityCard,
                resolvedSceneType,
                PortalCardType.CONTENT,
                contentCardDataProvider::currentContentCard,
                PortalContentCard::empty,
                CONTENT_CARD_DEGRADED)
                : null;

        return new PortalDashboardView(resolvedSceneType, identitySnapshot, todoSnapshot, messageSnapshot, contentSnapshot);
    }

    public PortalCardSnapshot<?> refreshCard(PortalSceneType sceneType, PortalCardType cardType) {
        PortalSceneType resolvedSceneType = sceneType == null ? PortalSceneType.HOME : sceneType;
        PortalCardType resolvedCardType = Objects.requireNonNull(cardType, "cardType must not be null");
        PortalIdentityCard identityCard = identityCardDataProvider.currentIdentity();
        return switch (resolvedCardType) {
            case IDENTITY -> refreshIdentitySnapshot(identityCard, resolvedSceneType);
            case TODO -> refreshPortalCard(
                    identityCard,
                    resolvedSceneType,
                    PortalCardType.TODO,
                    todoCardDataProvider::currentTodoCard,
                    PortalTodoCard::empty,
                    TODO_CARD_DEGRADED);
            case MESSAGE -> refreshPortalCard(
                    identityCard,
                    resolvedSceneType,
                    PortalCardType.MESSAGE,
                    messageCardDataProvider::currentMessageCard,
                    PortalMessageCard::empty,
                    MESSAGE_CARD_DEGRADED);
            case CONTENT -> refreshPortalCard(
                    identityCard,
                    resolvedSceneType,
                    PortalCardType.CONTENT,
                    contentCardDataProvider::currentContentCard,
                    PortalContentCard::empty,
                    CONTENT_CARD_DEGRADED);
        };
    }

    private PortalCardSnapshot<PortalIdentityCard> resolveIdentitySnapshot(
            PortalIdentityCard identityCard,
            PortalSceneType sceneType
    ) {
        PortalAggregationSnapshotKey snapshotKey = PortalAggregationSnapshotKey.of(identityCard, sceneType, PortalCardType.IDENTITY);
        PortalCardSnapshot<?> cachedSnapshot = readCachedSnapshot(snapshotKey);
        if (cachedSnapshot == null || cachedSnapshot.isStale()) {
            return refreshIdentitySnapshot(identityCard, sceneType);
        }
        return castSnapshot(cachedSnapshot);
    }

    private PortalCardSnapshot<PortalIdentityCard> refreshIdentitySnapshot(
            PortalIdentityCard identityCard,
            PortalSceneType sceneType
    ) {
        PortalAggregationSnapshotKey snapshotKey = PortalAggregationSnapshotKey.of(identityCard, sceneType, PortalCardType.IDENTITY);
        PortalCardSnapshot<PortalIdentityCard> snapshot =
                PortalCardSnapshot.ready(snapshotKey, PortalCardType.IDENTITY, identityCard, now());
        saveSnapshotSafely(snapshot);
        publishRefreshedSafely(snapshot);
        return snapshot;
    }

    private <T> PortalCardSnapshot<T> readPortalCard(
            PortalIdentityCard identityCard,
            PortalSceneType sceneType,
            PortalCardType cardType,
            Supplier<T> loader,
            Supplier<T> emptyFactory,
            String failedMessage
    ) {
        PortalAggregationSnapshotKey snapshotKey = PortalAggregationSnapshotKey.of(identityCard, sceneType, cardType);
        PortalCardSnapshot<?> cachedSnapshot = readCachedSnapshot(snapshotKey);
        if (cachedSnapshot == null || cachedSnapshot.isStale()) {
            return refreshPortalCard(identityCard, sceneType, cardType, loader, emptyFactory, failedMessage);
        }
        return castSnapshot(cachedSnapshot);
    }

    private <T> PortalCardSnapshot<T> refreshPortalCard(
            PortalIdentityCard identityCard,
            PortalSceneType sceneType,
            PortalCardType cardType,
            Supplier<T> loader,
            Supplier<T> emptyFactory,
            String failedMessage
    ) {
        PortalAggregationSnapshotKey snapshotKey = PortalAggregationSnapshotKey.of(identityCard, sceneType, cardType);
        try {
            T data = loader.get();
            PortalCardSnapshot<T> snapshot = PortalCardSnapshot.ready(snapshotKey, cardType, data, now());
            saveSnapshotSafely(snapshot);
            publishRefreshedSafely(snapshot);
            return snapshot;
        } catch (RuntimeException ex) {
            PortalCardSnapshot<T> snapshot = PortalCardSnapshot.failed(
                    snapshotKey,
                    cardType,
                    emptyFactory.get(),
                    failedMessage,
                    now()
            );
            saveSnapshotSafely(snapshot);
            publishFailedSafely(snapshot);
            return snapshot;
        }
    }

    private PortalCardSnapshot<?> readCachedSnapshot(PortalAggregationSnapshotKey snapshotKey) {
        try {
            return snapshotRepository.findByKey(snapshotKey).orElse(null);
        } catch (RuntimeException ex) {
            log.warn("Failed to read portal snapshot cache for key {}", snapshotKey.asCacheKey(), ex);
            return null;
        }
    }

    private void saveSnapshotSafely(PortalCardSnapshot<?> snapshot) {
        try {
            snapshotRepository.save(snapshot);
        } catch (RuntimeException ex) {
            log.warn("Failed to persist portal snapshot {}", snapshot.snapshotKey().asCacheKey(), ex);
        }
    }

    private void publishRefreshedSafely(PortalCardSnapshot<?> snapshot) {
        try {
            domainEventPublisher.publish(PortalSnapshotRefreshedEvent.from(snapshot));
        } catch (RuntimeException ex) {
            log.warn("Failed to publish portal snapshot refreshed event for {}", snapshot.snapshotKey().asCacheKey(), ex);
        }
    }

    private void publishFailedSafely(PortalCardSnapshot<?> snapshot) {
        try {
            domainEventPublisher.publish(PortalSnapshotFailedEvent.from(snapshot));
        } catch (RuntimeException ex) {
            log.warn("Failed to publish portal snapshot failed event for {}", snapshot.snapshotKey().asCacheKey(), ex);
        }
    }

    private Set<PortalCardType> normalizeRequestedCards(Set<PortalCardType> requestedCards) {
        if (requestedCards == null || requestedCards.isEmpty()) {
            return EnumSet.of(PortalCardType.TODO, PortalCardType.MESSAGE);
        }
        return EnumSet.copyOf(requestedCards);
    }

    private Instant now() {
        return clock.instant();
    }

    @SuppressWarnings("unchecked")
    private static <T> PortalCardSnapshot<T> castSnapshot(PortalCardSnapshot<?> snapshot) {
        return (PortalCardSnapshot<T>) snapshot;
    }
}
