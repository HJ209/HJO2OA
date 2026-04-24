package com.hjo2oa.portal.portal.designer.domain;

import java.util.List;

public record PortalDesignerWidgetPaletteView(
        List<PortalDesignerWidgetPaletteEntryView> activeWidgets,
        List<PortalDesignerWidgetPaletteEntryView> disabledWidgets,
        int totalWidgets
) {
}
