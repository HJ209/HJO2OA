package com.hjo2oa.portal.portal.designer.interfaces;

import com.hjo2oa.portal.portal.model.application.SavePortalTemplateCanvasCommand;
import com.hjo2oa.portal.portal.model.domain.PortalLayoutRegion;
import com.hjo2oa.portal.portal.model.domain.PortalPage;
import com.hjo2oa.portal.portal.model.domain.PortalTemplateLayoutMode;
import com.hjo2oa.portal.portal.model.domain.PortalWidgetPlacement;
import com.hjo2oa.portal.widget.config.domain.WidgetCardType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public record SavePortalDesignerDraftRequest(
        @NotEmpty List<@Valid PageBody> pages
) {

    public SavePortalTemplateCanvasCommand toCommand(String templateId) {
        return new SavePortalTemplateCanvasCommand(
                templateId,
                pages.stream().map(PageBody::toDomain).toList()
        );
    }

    public record PageBody(
            @NotBlank @Size(max = 128) String pageId,
            @NotBlank @Size(max = 128) String pageCode,
            @NotBlank @Size(max = 256) String title,
            boolean defaultPage,
            @NotNull PortalTemplateLayoutMode layoutMode,
            @NotEmpty List<@Valid RegionBody> regions
    ) {

        private PortalPage toDomain() {
            return new PortalPage(
                    pageId,
                    pageCode,
                    title,
                    defaultPage,
                    layoutMode,
                    regions.stream().map(RegionBody::toDomain).toList()
            );
        }
    }

    public record RegionBody(
            @NotBlank @Size(max = 128) String regionId,
            @NotBlank @Size(max = 128) String regionCode,
            @NotBlank @Size(max = 256) String title,
            boolean required,
            @NotEmpty List<@Valid PlacementBody> placements
    ) {

        private PortalLayoutRegion toDomain() {
            return new PortalLayoutRegion(
                    regionId,
                    regionCode,
                    title,
                    required,
                    placements.stream().map(PlacementBody::toDomain).toList()
            );
        }
    }

    public record PlacementBody(
            @NotBlank @Size(max = 128) String placementId,
            @NotBlank @Size(max = 128) String placementCode,
            @NotBlank @Size(max = 128) String widgetCode,
            @NotNull WidgetCardType cardType,
            @Min(1) int orderNo,
            boolean hiddenByDefault,
            boolean collapsedByDefault,
            Map<String, String> overrideProps
    ) {

        private PortalWidgetPlacement toDomain() {
            return new PortalWidgetPlacement(
                    placementId,
                    placementCode,
                    widgetCode,
                    cardType,
                    orderNo,
                    hiddenByDefault,
                    collapsedByDefault,
                    overrideProps == null ? Map.of() : overrideProps
            );
        }
    }
}
