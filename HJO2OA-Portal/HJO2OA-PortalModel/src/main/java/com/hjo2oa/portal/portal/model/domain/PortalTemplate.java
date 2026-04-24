package com.hjo2oa.portal.portal.model.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record PortalTemplate(
        String templateId,
        String tenantId,
        String templateCode,
        String displayName,
        PortalPublicationSceneType sceneType,
        List<PortalPage> pages,
        List<PortalTemplateVersion> versions,
        List<PortalTemplatePublishedCanvasSnapshot> publishedSnapshots,
        Instant createdAt,
        Instant updatedAt
) {

    public PortalTemplate {
        templateId = requireText(templateId, "templateId");
        tenantId = requireText(tenantId, "tenantId");
        templateCode = requireText(templateCode, "templateCode");
        displayName = requireText(displayName, "displayName");
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        pages = List.copyOf(Objects.requireNonNull(pages, "pages must not be null"));
        versions = List.copyOf(Objects.requireNonNull(versions, "versions must not be null"));
        publishedSnapshots = List.copyOf(Objects.requireNonNullElse(publishedSnapshots, List.of()));
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static PortalTemplate create(
            String templateId,
            String tenantId,
            String templateCode,
            String displayName,
            PortalPublicationSceneType sceneType,
            Instant now
    ) {
        return new PortalTemplate(
                templateId,
                tenantId,
                templateCode,
                displayName,
                sceneType,
                PortalTemplateCanvasFactory.defaultPages(sceneType),
                List.of(PortalTemplateVersion.draft(1, now)),
                List.of(),
                now,
                now
        );
    }

    public PortalTemplate publishVersion(int versionNo, Instant now) {
        Optional<PortalTemplateVersion> existingVersion = version(versionNo);
        if (existingVersion.isPresent()) {
            PortalTemplateVersion version = existingVersion.orElseThrow();
            PortalTemplate publishedTemplate = replaceVersion(version.publish(now), now);
            if (version.status() == PortalTemplateVersionStatus.PUBLISHED) {
                return publishedTemplate.ensurePublishedSnapshot(versionNo, now);
            }
            return publishedTemplate.capturePublishedSnapshot(versionNo, now);
        }
        int latestVersionNo = latestVersionNo();
        if (versionNo != latestVersionNo + 1) {
            throw new IllegalStateException("New template versions must be published sequentially");
        }
        List<PortalTemplateVersion> nextVersions = versions.stream()
                .sorted(Comparator.comparingInt(PortalTemplateVersion::versionNo))
                .toList();
        return new PortalTemplate(
                templateId,
                tenantId,
                templateCode,
                displayName,
                sceneType,
                pages,
                PortalTemplate.<PortalTemplateVersion>append(
                        nextVersions,
                        PortalTemplateVersion.published(versionNo, now)
                ),
                PortalTemplate.<PortalTemplatePublishedCanvasSnapshot>append(
                        publishedSnapshots,
                        PortalTemplatePublishedCanvasSnapshot.capture(versionNo, pages, now)
                ),
                createdAt,
                now
        );
    }

    public PortalTemplate deprecateVersion(int versionNo, Instant now) {
        PortalTemplateVersion existingVersion = version(versionNo)
                .orElseThrow(() -> new IllegalArgumentException("Template version not found"));
        return replaceVersion(existingVersion.deprecate(now), now);
    }

    public Optional<PortalTemplateVersion> version(int versionNo) {
        return versions.stream()
                .filter(version -> version.versionNo() == versionNo)
                .findFirst();
    }

    public int latestVersionNo() {
        return versions.stream()
                .mapToInt(PortalTemplateVersion::versionNo)
                .max()
                .orElse(0);
    }

    public Integer publishedVersionNo() {
        return versions.stream()
                .filter(version -> version.status() == PortalTemplateVersionStatus.PUBLISHED)
                .map(PortalTemplateVersion::versionNo)
                .max(Integer::compareTo)
                .orElse(null);
    }

    public PortalTemplateView toView() {
        List<PortalTemplateVersionView> versionViews = versions.stream()
                .sorted(Comparator.comparingInt(PortalTemplateVersion::versionNo))
                .map(PortalTemplateVersion::toView)
                .toList();
        return new PortalTemplateView(
                templateId,
                tenantId,
                templateCode,
                displayName,
                sceneType,
                latestVersionNo(),
                publishedVersionNo(),
                createdAt,
                updatedAt,
                versionViews
        );
    }

    public PortalTemplateCanvasView toCanvasView() {
        return new PortalTemplateCanvasView(
                templateId,
                templateCode,
                sceneType,
                latestVersionNo(),
                publishedVersionNo(),
                pages.stream().map(PortalPage::toView).toList()
        );
    }

    public Optional<PortalTemplateCanvasView> toPublishedCanvasView() {
        Integer publishedVersionNo = publishedVersionNo();
        if (publishedVersionNo == null) {
            return Optional.empty();
        }
        return publishedSnapshot(publishedVersionNo).map(snapshot -> new PortalTemplateCanvasView(
                templateId,
                templateCode,
                sceneType,
                latestVersionNo(),
                publishedVersionNo,
                snapshot.pages().stream().map(PortalPage::toView).toList()
        ));
    }

    public PortalTemplate replaceCanvas(List<PortalPage> replacementPages, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        List<PortalPage> normalizedPages = normalizePages(replacementPages);
        validatePageCodes(normalizedPages);
        validateRegionCodes(normalizedPages);
        return new PortalTemplate(
                templateId,
                tenantId,
                templateCode,
                displayName,
                sceneType,
                normalizedPages,
                versions,
                publishedSnapshots,
                createdAt,
                now
        );
    }

    private PortalTemplate replaceVersion(PortalTemplateVersion replacement, Instant now) {
        List<PortalTemplateVersion> nextVersions = versions.stream()
                .sorted(Comparator.comparingInt(PortalTemplateVersion::versionNo))
                .map(version -> version.versionNo() == replacement.versionNo() ? replacement : version)
                .toList();
        return new PortalTemplate(
                templateId,
                tenantId,
                templateCode,
                displayName,
                sceneType,
                pages,
                nextVersions,
                publishedSnapshots,
                createdAt,
                now
        );
    }

    public Optional<PortalTemplatePublishedCanvasSnapshot> publishedSnapshot(int versionNo) {
        return publishedSnapshots.stream()
                .filter(snapshot -> snapshot.versionNo() == versionNo)
                .findFirst();
    }

    PortalTemplate capturePublishedSnapshot(int versionNo, Instant now) {
        return new PortalTemplate(
                templateId,
                tenantId,
                templateCode,
                displayName,
                sceneType,
                pages,
                versions,
                PortalTemplate.<PortalTemplatePublishedCanvasSnapshot>append(
                        publishedSnapshots.stream()
                                .filter(snapshot -> snapshot.versionNo() != versionNo)
                                .toList(),
                        PortalTemplatePublishedCanvasSnapshot.capture(versionNo, pages, now)
                ),
                createdAt,
                now
        );
    }

    private PortalTemplate ensurePublishedSnapshot(int versionNo, Instant now) {
        if (publishedSnapshot(versionNo).isPresent()) {
            return this;
        }
        return capturePublishedSnapshot(versionNo, now);
    }

    private static <T> List<T> append(
            List<T> values,
            T newValue
    ) {
        return java.util.stream.Stream.concat(values.stream(), java.util.stream.Stream.of(newValue))
                .toList();
    }

    static List<PortalPage> copyPages(List<PortalPage> pages) {
        List<PortalPage> copiedPages = List.copyOf(Objects.requireNonNull(pages, "pages must not be null"));
        if (copiedPages.isEmpty()) {
            throw new IllegalArgumentException("pages must not be empty");
        }
        return copiedPages.stream()
                .map(page -> new PortalPage(
                        page.pageId(),
                        page.pageCode(),
                        page.title(),
                        page.defaultPage(),
                        page.layoutMode(),
                        page.regions().stream()
                                .map(region -> new PortalLayoutRegion(
                                        region.regionId(),
                                        region.regionCode(),
                                        region.title(),
                                        region.required(),
                                        region.placements().stream()
                                                .map(placement -> new PortalWidgetPlacement(
                                                        placement.placementId(),
                                                        placement.placementCode(),
                                                        placement.widgetCode(),
                                                        placement.cardType(),
                                                        placement.orderNo(),
                                                        placement.hiddenByDefault(),
                                                        placement.collapsedByDefault(),
                                                        placement.overrideProps()
                                                ))
                                                .sorted(Comparator.comparingInt(PortalWidgetPlacement::orderNo))
                                                .toList()
                                ))
                                .toList()
                ))
                .toList();
    }

    private static List<PortalPage> normalizePages(List<PortalPage> pages) {
        return copyPages(pages);
    }

    private static void validatePageCodes(List<PortalPage> pages) {
        Set<String> pageCodes = new HashSet<>();
        for (PortalPage page : pages) {
            if (!pageCodes.add(page.pageCode())) {
                throw new IllegalArgumentException("Duplicate page code: " + page.pageCode());
            }
        }
    }

    private static void validateRegionCodes(List<PortalPage> pages) {
        Set<String> regionCodes = new HashSet<>();
        for (PortalPage page : pages) {
            for (PortalLayoutRegion region : page.regions()) {
                if (!regionCodes.add(region.regionCode())) {
                    throw new IllegalArgumentException("Duplicate region code: " + region.regionCode());
                }
            }
        }
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
