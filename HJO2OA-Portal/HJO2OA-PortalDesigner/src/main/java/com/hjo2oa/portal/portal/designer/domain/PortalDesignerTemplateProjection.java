package com.hjo2oa.portal.portal.designer.domain;

import com.hjo2oa.portal.portal.model.domain.PortalPublicationActivatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationOfflinedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationSceneType;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCreatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateDeprecatedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplatePublishedEvent;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateVersionStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record PortalDesignerTemplateProjection(
        String tenantId,
        String templateId,
        String templateCode,
        PortalPublicationSceneType sceneType,
        List<PortalDesignerTemplateVersionView> versions,
        List<String> activePublicationIds,
        Instant createdAt,
        Instant updatedAt
) {

    public PortalDesignerTemplateProjection {
        tenantId = requireText(tenantId, "tenantId");
        templateId = requireText(templateId, "templateId");
        templateCode = requireText(templateCode, "templateCode");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        versions = List.copyOf(Objects.requireNonNull(versions, "versions must not be null"));
        activePublicationIds = List.copyOf(Objects.requireNonNull(activePublicationIds, "activePublicationIds must not be null"));
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PortalDesignerTemplateProjection initialize(PortalTemplateCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Instant now = event.occurredAt();
        return new PortalDesignerTemplateProjection(
                event.tenantId(),
                event.templateId(),
                event.templateCode(),
                event.sceneType(),
                List.of(new PortalDesignerTemplateVersionView(
                        1,
                        PortalTemplateVersionStatus.DRAFT,
                        now,
                        null,
                        null
                )),
                List.of(),
                now,
                now
        );
    }

    public PortalDesignerTemplateProjection applyTemplatePublished(PortalTemplatePublishedEvent event) {
        return new PortalDesignerTemplateProjection(
                tenantId,
                templateId,
                templateCode,
                sceneType,
                upsertVersion(new PortalDesignerTemplateVersionView(
                        event.versionNo(),
                        PortalTemplateVersionStatus.PUBLISHED,
                        existingVersion(event.versionNo()).map(PortalDesignerTemplateVersionView::createdAt).orElse(event.occurredAt()),
                        event.occurredAt(),
                        null
                )),
                activePublicationIds,
                createdAt,
                event.occurredAt()
        );
    }

    public PortalDesignerTemplateProjection applyTemplateDeprecated(PortalTemplateDeprecatedEvent event) {
        PortalDesignerTemplateVersionView existingVersion = existingVersion(event.versionNo())
                .orElse(new PortalDesignerTemplateVersionView(
                        event.versionNo(),
                        PortalTemplateVersionStatus.PUBLISHED,
                        event.occurredAt(),
                        event.occurredAt(),
                        null
                ));
        return new PortalDesignerTemplateProjection(
                tenantId,
                templateId,
                templateCode,
                sceneType,
                upsertVersion(new PortalDesignerTemplateVersionView(
                        event.versionNo(),
                        PortalTemplateVersionStatus.DEPRECATED,
                        existingVersion.createdAt(),
                        existingVersion.publishedAt(),
                        event.occurredAt()
                )),
                activePublicationIds,
                createdAt,
                event.occurredAt()
        );
    }

    public PortalDesignerTemplateProjection activatePublication(PortalPublicationActivatedEvent event) {
        if (activePublicationIds.contains(event.publicationId())) {
            return this;
        }
        return new PortalDesignerTemplateProjection(
                tenantId,
                templateId,
                templateCode,
                sceneType,
                versions,
                Stream.concat(activePublicationIds.stream(), Stream.of(event.publicationId())).toList(),
                createdAt,
                event.occurredAt()
        );
    }

    public PortalDesignerTemplateProjection offlinePublication(PortalPublicationOfflinedEvent event) {
        if (!activePublicationIds.contains(event.publicationId())) {
            return this;
        }
        return new PortalDesignerTemplateProjection(
                tenantId,
                templateId,
                templateCode,
                sceneType,
                versions,
                activePublicationIds.stream()
                        .filter(publicationId -> !publicationId.equals(event.publicationId()))
                        .toList(),
                createdAt,
                event.occurredAt()
        );
    }

    public PortalDesignerTemplateStatusView toView() {
        Integer latestVersionNo = versions.stream()
                .map(PortalDesignerTemplateVersionView::versionNo)
                .max(Integer::compareTo)
                .orElse(null);
        Integer publishedVersionNo = versions.stream()
                .filter(version -> version.status() == PortalTemplateVersionStatus.PUBLISHED)
                .map(PortalDesignerTemplateVersionView::versionNo)
                .max(Integer::compareTo)
                .orElse(null);
        return new PortalDesignerTemplateStatusView(
                templateId,
                templateCode,
                sceneType,
                latestVersionNo,
                publishedVersionNo,
                versions.stream()
                        .sorted(Comparator.comparingInt(PortalDesignerTemplateVersionView::versionNo))
                        .toList(),
                activePublicationIds,
                !activePublicationIds.isEmpty(),
                createdAt,
                updatedAt
        );
    }

    private Optional<PortalDesignerTemplateVersionView> existingVersion(int versionNo) {
        return versions.stream()
                .filter(version -> version.versionNo() == versionNo)
                .findFirst();
    }

    private List<PortalDesignerTemplateVersionView> upsertVersion(PortalDesignerTemplateVersionView replacement) {
        if (existingVersion(replacement.versionNo()).isPresent()) {
            return versions.stream()
                    .map(version -> version.versionNo() == replacement.versionNo() ? replacement : version)
                    .toList();
        }
        return Stream.concat(versions.stream(), Stream.of(replacement)).toList();
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
