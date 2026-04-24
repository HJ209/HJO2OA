package com.hjo2oa.portal.personalization.interfaces;

import com.hjo2oa.portal.personalization.application.SavePersonalizationProfileCommand;
import com.hjo2oa.portal.personalization.domain.PersonalizationProfileScope;
import com.hjo2oa.portal.personalization.domain.PersonalizationSceneType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SavePersonalizationProfileRequest(
        @NotNull PersonalizationSceneType sceneType,
        PersonalizationProfileScope scope,
        String assignmentId,
        @Size(max = 64) String themeCode,
        List<@Size(max = 128) String> widgetOrderOverride,
        List<@Size(max = 128) String> hiddenPlacementCodes,
        List<@Valid QuickAccessEntryRequest> quickAccessEntries
) {

    public SavePersonalizationProfileCommand toCommand() {
        return new SavePersonalizationProfileCommand(
                sceneType,
                scope,
                assignmentId,
                themeCode,
                widgetOrderOverride,
                hiddenPlacementCodes,
                quickAccessEntries == null ? List.of() : quickAccessEntries.stream().map(QuickAccessEntryRequest::toDomain).toList()
        );
    }
}
