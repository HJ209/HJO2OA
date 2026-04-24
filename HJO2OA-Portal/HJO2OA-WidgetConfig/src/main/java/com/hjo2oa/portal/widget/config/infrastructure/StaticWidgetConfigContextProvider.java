package com.hjo2oa.portal.widget.config.infrastructure;

import com.hjo2oa.portal.widget.config.domain.WidgetConfigContext;
import com.hjo2oa.portal.widget.config.domain.WidgetConfigContextProvider;
import org.springframework.stereotype.Component;

@Component
public class StaticWidgetConfigContextProvider implements WidgetConfigContextProvider {

    @Override
    public WidgetConfigContext currentContext() {
        return new WidgetConfigContext("tenant-1", "portal-admin");
    }
}
