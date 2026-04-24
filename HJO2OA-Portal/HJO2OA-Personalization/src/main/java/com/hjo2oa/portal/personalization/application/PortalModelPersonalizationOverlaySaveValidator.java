package com.hjo2oa.portal.personalization.application;

import com.hjo2oa.portal.personalization.domain.PersonalizationIdentityContext;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import com.hjo2oa.portal.personalization.domain.ValidatedPersonalizationOverlay;
import com.hjo2oa.portal.portal.model.application.PortalPublicationApplicationService;
import com.hjo2oa.portal.portal.model.application.PortalTemplateCanvasApplicationService;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegionView;
import com.hjo2oa.portal.portal.model.domain.PortalPageView;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationStatus;
import com.hjo2oa.portal.portal.model.domain.PortalPublicationView;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateCanvasView;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacementView;
import com.hjo2oa.portal.widget.config.application.WidgetDefinitionApplicationService;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionStatus;
import com.hjo2oa.portal.widget.config.domain.WidgetDefinitionView;
import com.hjo2oa.portal.widget.config.domain.WidgetSceneType;
import com.hjo2oa.shared.kernel.BizException;
import com.hjo2oa.shared.kernel.SharedErrorDescriptors;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PortalModelPersonalizationOverlaySaveValidator implements PersonalizationOverlaySaveValidator {

    private final PortalPublicationApplicationService publicationApplicationService;
    private final PortalTemplateCanvasApplicationService templateCanvasApplicationService;
    private final WidgetDefinitionApplicationService widgetDefinitionApplicationService;

    public PortalModelPersonalizationOverlaySaveValidator(
            PortalPublicationApplicationService publicationApplicationService,
            PortalTemplateCanvasApplicationService templateCanvasApplicationService,
            WidgetDefinitionApplicationService widgetDefinitionApplicationService
    ) {
        this.publicationApplicationService = Objects.requireNonNull(
                publicationApplicationService,
                "publicationApplicationService must not be null"
        );
        this.templateCanvasApplicationService = Objects.requireNonNull(
                templateCanvasApplicationService,
                "templateCanvasApplicationService must not be null"
        );
        this.widgetDefinitionApplicationService = Objects.requireNonNull(
                widgetDefinitionApplicationService,
                "widgetDefinitionApplicationService must not be null"
        );
    }

    @Override
    public ValidatedPersonalizationOverlay validate(
            PersonalizationSceneType sceneType,
            PersonalizationIdentityContext identityContext,
            String resolvedBasePublicationId,
            String existingBasePublicationId,
            List<String> widgetOrderOverride,
            List<String> hiddenPlacementCodes
    ) {
        Objects.requireNonNull(sceneType, "sceneType must not be null");
        Objects.requireNonNull(identityContext, "identityContext must not be null");
        String normalizedResolvedBasePublicationId = requireText(resolvedBasePublicationId, "resolvedBasePublicationId");
        String normalizedExistingBasePublicationId = normalizeOptional(existingBasePublicationId);
        if (normalizedExistingBasePublicationId != null
                && !normalizedExistingBasePublicationId.equals(normalizedResolvedBasePublicationId)) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "personalization profile is based on stale publication "
                            + normalizedExistingBasePublicationId
                            + "; live publication is "
                            + normalizedResolvedBasePublicationId
            );
        }

        PortalPublicationView publication = publicationApplicationService.current(normalizedResolvedBasePublicationId)
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "live publication not found: publicationId=" + normalizedResolvedBasePublicationId
                ));
        if (publication.status() != PortalPublicationStatus.ACTIVE) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "live publication is not active: publicationId=" + publication.publicationId()
                            + ", status=" + publication.status()
            );
        }

        PortalTemplateCanvasView canvas = templateCanvasApplicationService.currentPublished(publication.templateId())
                .orElseThrow(() -> new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "live publication has no published canvas: publicationId="
                                + publication.publicationId()
                                + ", templateId="
                                + publication.templateId()
                ));
        OverlayPlacementContext placementContext = placementContext(publication, canvas);
        List<String> resolvedWidgetOrderOverride = resolvePlacementReferences(
                widgetOrderOverride,
                placementContext,
                "widgetOrderOverride"
        );
        List<String> resolvedHiddenPlacementCodes = resolvePlacementReferences(
                hiddenPlacementCodes,
                placementContext,
                "hiddenPlacementCodes"
        );
        ensureNoOverlayConflicts(resolvedWidgetOrderOverride, resolvedHiddenPlacementCodes);
        ensureHiddenPlacementsAllowed(sceneType, placementContext, resolvedHiddenPlacementCodes);
        return new ValidatedPersonalizationOverlay(
                normalizedResolvedBasePublicationId,
                resolvedWidgetOrderOverride,
                resolvedHiddenPlacementCodes
        );
    }

    private OverlayPlacementContext placementContext(
            PortalPublicationView publication,
            PortalTemplateCanvasView canvas
    ) {
        Optional<PortalPageView> page = canvas.pages().stream()
                .filter(PortalPageView::defaultPage)
                .findFirst()
                .or(() -> canvas.pages().stream().findFirst());
        if (page.isEmpty()) {
            return new OverlayPlacementContext(publication.publicationId(), Map.of(), Map.of());
        }

        LinkedHashMap<String, OverlayPlacementBoundary> placementsByCode = new LinkedHashMap<>();
        LinkedHashMap<String, List<OverlayPlacementBoundary>> placementsByWidgetCode = new LinkedHashMap<>();
        for (PortalLayoutRegionView region : page.orElseThrow().regions()) {
            boolean onlyPlacementInRequiredRegion = region.required() && region.placements().size() == 1;
            for (PortalWidgetPlacementView placement : region.placements()) {
                OverlayPlacementBoundary boundary = new OverlayPlacementBoundary(
                        placement.placementCode(),
                        placement.widgetCode(),
                        region.regionCode(),
                        region.required(),
                        onlyPlacementInRequiredRegion
                );
                placementsByCode.putIfAbsent(boundary.placementCode(), boundary);
                placementsByWidgetCode.computeIfAbsent(boundary.widgetCode(), ignored -> new ArrayList<>())
                        .add(boundary);
            }
        }
        return new OverlayPlacementContext(
                publication.publicationId(),
                Map.copyOf(placementsByCode),
                immutableCopy(placementsByWidgetCode)
        );
    }

    private Map<String, List<OverlayPlacementBoundary>> immutableCopy(
            Map<String, List<OverlayPlacementBoundary>> placementsByWidgetCode
    ) {
        LinkedHashMap<String, List<OverlayPlacementBoundary>> copied = new LinkedHashMap<>();
        placementsByWidgetCode.forEach((widgetCode, boundaries) -> copied.put(widgetCode, List.copyOf(boundaries)));
        return Map.copyOf(copied);
    }

    private List<String> resolvePlacementReferences(
            List<String> values,
            OverlayPlacementContext placementContext,
            String fieldName
    ) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> resolvedPlacementCodes = new LinkedHashSet<>();
        LinkedHashSet<String> unknownReferences = new LinkedHashSet<>();
        LinkedHashSet<String> ambiguousReferences = new LinkedHashSet<>();
        LinkedHashSet<String> duplicatePlacementCodes = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + " must not contain null");
            }
            String normalizedValue = requireText(value, fieldName);
            String resolvedPlacementCode = resolvePlacementCode(
                    normalizedValue,
                    placementContext,
                    unknownReferences,
                    ambiguousReferences
            );
            if (resolvedPlacementCode != null && !resolvedPlacementCodes.add(resolvedPlacementCode)) {
                duplicatePlacementCodes.add(resolvedPlacementCode);
            }
        }
        if (!unknownReferences.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    fieldName + " contains unknown live placement references for publicationId="
                            + placementContext.publicationId()
                            + ": "
                            + String.join(", ", unknownReferences)
            );
        }
        if (!ambiguousReferences.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    fieldName + " contains ambiguous widget references for publicationId="
                            + placementContext.publicationId()
                            + ": "
                            + String.join(", ", ambiguousReferences)
            );
        }
        if (!duplicatePlacementCodes.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "duplicate " + fieldName + " placement codes: " + String.join(", ", duplicatePlacementCodes)
            );
        }
        return List.copyOf(resolvedPlacementCodes);
    }

    private String resolvePlacementCode(
            String value,
            OverlayPlacementContext placementContext,
            LinkedHashSet<String> unknownReferences,
            LinkedHashSet<String> ambiguousReferences
    ) {
        if (placementContext.placementsByCode().containsKey(value)) {
            return value;
        }
        List<OverlayPlacementBoundary> widgetMatches = placementContext.placementsByWidgetCode().get(value);
        if (widgetMatches == null || widgetMatches.isEmpty()) {
            unknownReferences.add(value);
            return null;
        }
        if (widgetMatches.size() > 1) {
            ambiguousReferences.add(value);
            return null;
        }
        return widgetMatches.get(0).placementCode();
    }

    private void ensureNoOverlayConflicts(
            List<String> widgetOrderOverride,
            List<String> hiddenPlacementCodes
    ) {
        if (widgetOrderOverride.isEmpty() || hiddenPlacementCodes.isEmpty()) {
            return;
        }
        LinkedHashSet<String> conflicts = new LinkedHashSet<>(widgetOrderOverride);
        conflicts.retainAll(hiddenPlacementCodes);
        if (!conflicts.isEmpty()) {
            throw new BizException(
                    SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                    "widgetOrderOverride conflicts with hiddenPlacementCodes for placementCode(s): "
                            + String.join(", ", conflicts)
            );
        }
    }

    private void ensureHiddenPlacementsAllowed(
            PersonalizationSceneType sceneType,
            OverlayPlacementContext placementContext,
            List<String> hiddenPlacementCodes
    ) {
        if (hiddenPlacementCodes.isEmpty()) {
            return;
        }
        Map<String, WidgetDefinitionView> widgetDefinitionsByCode = widgetDefinitionApplicationService.list(
                mapWidgetSceneType(sceneType),
                WidgetDefinitionStatus.ACTIVE
        ).stream().collect(java.util.stream.Collectors.toMap(
                WidgetDefinitionView::widgetCode,
                definition -> definition,
                (left, right) -> left,
                LinkedHashMap::new
        ));
        for (String placementCode : hiddenPlacementCodes) {
            OverlayPlacementBoundary boundary = placementContext.placementsByCode().get(placementCode);
            if (boundary == null) {
                continue;
            }
            if (boundary.onlyPlacementInRequiredRegion()) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "hiddenPlacementCodes cannot hide required placementCode="
                                + placementCode
                                + " in required region "
                                + boundary.regionCode()
                );
            }
            WidgetDefinitionView widgetDefinition = widgetDefinitionsByCode.get(boundary.widgetCode());
            if (widgetDefinition == null) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "hiddenPlacementCodes cannot resolve live widget boundary for placementCode="
                                + placementCode
                                + ", widgetCode="
                                + boundary.widgetCode()
                );
            }
            if (!widgetDefinition.allowHide()) {
                throw new BizException(
                        SharedErrorDescriptors.BUSINESS_RULE_VIOLATION,
                        "hiddenPlacementCodes cannot hide placementCode="
                                + placementCode
                                + " because widgetCode="
                                + boundary.widgetCode()
                                + " does not allow hide"
                );
            }
        }
    }

    private WidgetSceneType mapWidgetSceneType(PersonalizationSceneType sceneType) {
        return switch (sceneType) {
            case HOME -> WidgetSceneType.HOME;
            case OFFICE_CENTER -> WidgetSceneType.OFFICE_CENTER;
            case MOBILE_WORKBENCH -> WidgetSceneType.MOBILE_WORKBENCH;
        };
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private record OverlayPlacementContext(
            String publicationId,
            Map<String, OverlayPlacementBoundary> placementsByCode,
            Map<String, List<OverlayPlacementBoundary>> placementsByWidgetCode
    ) {
    }

    private record OverlayPlacementBoundary(
            String placementCode,
            String widgetCode,
            String regionCode,
            boolean regionRequired,
            boolean onlyPlacementInRequiredRegion
    ) {
    }
}
