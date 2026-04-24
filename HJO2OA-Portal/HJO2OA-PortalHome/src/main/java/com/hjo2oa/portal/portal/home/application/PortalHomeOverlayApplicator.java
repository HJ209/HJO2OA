package com.hjo2oa.portal.portal.home.application;

import com.hjo2oa.portal.portal.home.domain.PortalHomeCardTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomePageTemplate;
import com.hjo2oa.portal.portal.home.domain.PortalHomeRegionTemplate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

public class PortalHomeOverlayApplicator {

    public PortalHomeOverlayApplicationResult apply(
            PortalHomePageTemplate template,
            String resolvedPublicationId,
            PortalHomePersonalizationOverlay overlay
    ) {
        Objects.requireNonNull(template, "template must not be null");
        String normalizedResolvedPublicationId = normalizeOptional(resolvedPublicationId);
        if (overlay == null) {
            return PortalHomeOverlayApplicationResult.bypassed(
                    template,
                    null,
                    normalizedResolvedPublicationId,
                    "overlay-unavailable"
            );
        }
        String baselinePublicationId = overlay.basePublicationId();
        if (baselinePublicationId == null) {
            return PortalHomeOverlayApplicationResult.bypassed(
                    template,
                    null,
                    normalizedResolvedPublicationId,
                    "overlay-unavailable"
            );
        }
        if (normalizedResolvedPublicationId == null) {
            return PortalHomeOverlayApplicationResult.bypassed(
                    template,
                    baselinePublicationId,
                    null,
                    "live-publication-unresolved"
            );
        }
        if (!baselinePublicationId.equals(normalizedResolvedPublicationId)) {
            return PortalHomeOverlayApplicationResult.bypassed(
                    template,
                    baselinePublicationId,
                    normalizedResolvedPublicationId,
                    "baseline-publication-mismatch"
            );
        }
        Set<String> hiddenPlacementCodes = normalizeCodeSet(overlay.hiddenPlacementCodes());
        Map<String, Integer> widgetOrderPositions = normalizeCodePositions(overlay.widgetOrderOverride());
        if (hiddenPlacementCodes.isEmpty() && widgetOrderPositions.isEmpty()) {
            return PortalHomeOverlayApplicationResult.bypassed(
                    template,
                    baselinePublicationId,
                    normalizedResolvedPublicationId,
                    "overlay-empty"
            );
        }

        List<PortalHomeRegionTemplate> overlaidRegions = template.regions().stream()
                .map(region -> applyOverlay(region, hiddenPlacementCodes, widgetOrderPositions))
                .toList();
        if (overlaidRegions.equals(template.regions())) {
            return PortalHomeOverlayApplicationResult.bypassed(
                    template,
                    baselinePublicationId,
                    normalizedResolvedPublicationId,
                    "overlay-noop"
            );
        }
        return PortalHomeOverlayApplicationResult.applied(
                new PortalHomePageTemplate(
                        template.sceneType(),
                        template.layoutType(),
                        template.branding(),
                        template.navigation(),
                        overlaidRegions,
                        template.footer(),
                        template.sourceTemplateMetadata()
                ),
                baselinePublicationId,
                normalizedResolvedPublicationId,
                "publication-matched"
        );
    }

    private PortalHomeRegionTemplate applyOverlay(
            PortalHomeRegionTemplate regionTemplate,
            Set<String> hiddenPlacementCodes,
            Map<String, Integer> widgetOrderPositions
    ) {
        List<PortalHomeCardTemplate> visibleCards = regionTemplate.cards().stream()
                .filter(card -> !isHidden(card, hiddenPlacementCodes))
                .toList();
        List<PortalHomeCardTemplate> orderedCards = reorderWithinRegion(visibleCards, widgetOrderPositions);
        if (orderedCards.equals(regionTemplate.cards())) {
            return regionTemplate;
        }
        return new PortalHomeRegionTemplate(
                regionTemplate.regionCode(),
                regionTemplate.title(),
                regionTemplate.description(),
                orderedCards
        );
    }

    private boolean isHidden(PortalHomeCardTemplate cardTemplate, Set<String> hiddenPlacementCodes) {
        return cardTemplate.sourcePlacementCode() != null
                && hiddenPlacementCodes.contains(cardTemplate.sourcePlacementCode());
    }

    private List<PortalHomeCardTemplate> reorderWithinRegion(
            List<PortalHomeCardTemplate> cards,
            Map<String, Integer> widgetOrderPositions
    ) {
        if (cards.size() < 2 || widgetOrderPositions.isEmpty()) {
            return cards;
        }
        return IntStream.range(0, cards.size())
                .boxed()
                .sorted(Comparator.<Integer>comparingInt(index -> orderRank(cards.get(index), widgetOrderPositions))
                        .thenComparingInt(index -> index))
                .map(cards::get)
                .toList();
    }

    private int orderRank(
            PortalHomeCardTemplate cardTemplate,
            Map<String, Integer> widgetOrderPositions
    ) {
        Integer directSourceWidgetMatch = widgetOrderPositions.get(cardTemplate.sourceWidgetCode());
        if (directSourceWidgetMatch != null) {
            return directSourceWidgetMatch;
        }
        Integer sourcePlacementMatch = widgetOrderPositions.get(cardTemplate.sourcePlacementCode());
        if (sourcePlacementMatch != null) {
            return sourcePlacementMatch;
        }
        Integer cardCodeMatch = widgetOrderPositions.get(cardTemplate.cardCode());
        if (cardCodeMatch != null) {
            return cardCodeMatch;
        }
        return Integer.MAX_VALUE;
    }

    private Set<String> normalizeCodeSet(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .map(this::normalizeOptional)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private Map<String, Integer> normalizeCodePositions(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Integer> positions = new LinkedHashMap<>();
        for (String value : values) {
            String normalizedValue = normalizeOptional(value);
            if (normalizedValue != null) {
                positions.putIfAbsent(normalizedValue, positions.size());
            }
        }
        return Map.copyOf(positions);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
